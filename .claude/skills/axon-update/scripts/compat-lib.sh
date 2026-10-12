# shellcheck shell=bash
# Helpers for the "old data, new version" compatibility run of an Axon 5 application.
# Source it from bash or zsh:   . path/to/compat-lib.sh
#
# Everything is a function (no commands stored in variables), so it behaves the same in
# bash and zsh. Configure with environment variables, before or after sourcing:
#
#   AXC_DIR            work dir for logs, pid file and snapshots (default: $TMPDIR/axon-compat)
#   AXC_PORT           HTTP port the app listens on (default 8080), passed as SERVER_PORT
#   AXC_BASE_URL       base URL for axc_req (default http://localhost:$AXC_PORT)
#   AXC_HEALTH_PATH    health endpoint (default /actuator/health)
#   AXC_START_TIMEOUT  seconds to wait for health (default 120)
#   AXC_PROFILES       Spring profiles (SPRING_PROFILES_ACTIVE); empty = default profile
#   AXC_DB_URL / AXC_DB_USER / AXC_DB_PASSWORD
#                      JDBC datasource for the app (SPRING_DATASOURCE_*), if set
#   AXC_JAVA_OPTS      extra JVM options
#   AXC_PG_CONTAINER   docker container running Postgres (psql via docker exec), or
#   AXC_PG_DSN         a libpq connection string for a local psql
#   AXC_PG_DB / AXC_PG_USER   database and user for AXC_PG_CONTAINER
#
# Non-Spring apps: AXC_START_CMD overrides how the jar is started; it is evaluated with
# $AXC_JAR and $AXC_LABEL set. Any other env var you prefix to axc_start
# (FOO=bar axc_start ...) is visible to the app.

: "${AXC_DIR:=${TMPDIR:-/tmp}/axon-compat}"
: "${AXC_PORT:=8080}"
: "${AXC_HEALTH_PATH:=/actuator/health}"
: "${AXC_START_TIMEOUT:=120}"
mkdir -p "$AXC_DIR"

axc_base_url() { printf '%s' "${AXC_BASE_URL:-http://localhost:$AXC_PORT}"; }

# axc_start <jar> <label> [app args...]  - start the app in the background, wait for health.
axc_start() {
  local jar="$1" label="$2"
  shift 2
  if [ -f "$AXC_DIR/app.pid" ] && kill -0 "$(cat "$AXC_DIR/app.pid")" 2>/dev/null; then
    echo "axc_start: an app is already running (pid $(cat "$AXC_DIR/app.pid")); run axc_stop first" >&2
    return 1
  fi
  (
    export SERVER_PORT="$AXC_PORT" LOGGING_FILE_NAME="$AXC_DIR/$label.log"
    [ -n "${AXC_PROFILES:-}" ] && export SPRING_PROFILES_ACTIVE="$AXC_PROFILES"
    [ -n "${AXC_DB_URL:-}" ] && export SPRING_DATASOURCE_URL="$AXC_DB_URL"
    [ -n "${AXC_DB_USER:-}" ] && export SPRING_DATASOURCE_USERNAME="$AXC_DB_USER"
    [ -n "${AXC_DB_PASSWORD:-}" ] && export SPRING_DATASOURCE_PASSWORD="$AXC_DB_PASSWORD"
    if [ -n "${AXC_START_CMD:-}" ]; then
      export AXC_JAR="$jar" AXC_LABEL="$label"
      exec nohup sh -c "$AXC_START_CMD"
    fi
    # eval splits AXC_JAVA_OPTS into words the same way in bash and zsh
    eval "exec nohup java ${AXC_JAVA_OPTS:-} -jar \"\$jar\" \"\$@\""
  ) > "$AXC_DIR/$label.stdout" 2>&1 &
  echo $! > "$AXC_DIR/app.pid"
  echo "[$label] pid $(cat "$AXC_DIR/app.pid"), log $AXC_DIR/$label.log"
  axc_wait_healthy "$label"
}

# axc_wait_healthy [label]  - poll the health endpoint until it answers 2xx.
axc_wait_healthy() {
  local label="${1:-app}" i=0 url
  url="$(axc_base_url)$AXC_HEALTH_PATH"
  while [ "$i" -lt "$AXC_START_TIMEOUT" ]; do
    if curl -sf -o /dev/null "$url"; then
      echo "[$label] healthy after ${i}s"
      return 0
    fi
    if ! kill -0 "$(cat "$AXC_DIR/app.pid" 2>/dev/null)" 2>/dev/null; then
      echo "[$label] process exited during startup:" >&2
      tail -40 "$AXC_DIR/$label.stdout" >&2
      return 1
    fi
    sleep 1
    i=$((i + 1))
  done
  echo "[$label] not healthy after ${AXC_START_TIMEOUT}s ($url)" >&2
  tail -40 "$AXC_DIR/$label.stdout" >&2
  return 1
}

# axc_stop  - graceful stop (SIGTERM releases token claims), SIGKILL after 60s.
axc_stop() {
  local pid i=0
  pid="$(cat "$AXC_DIR/app.pid" 2>/dev/null)" || { echo "axc_stop: no pid file"; return 0; }
  kill "$pid" 2>/dev/null || { echo "axc_stop: not running"; return 0; }
  while kill -0 "$pid" 2>/dev/null; do
    sleep 1
    i=$((i + 1))
    if [ "$i" -ge 60 ]; then
      echo "axc_stop: still running after 60s, killing" >&2
      kill -9 "$pid" 2>/dev/null
    fi
  done
  rm -f "$AXC_DIR/app.pid"
  echo "app stopped"
}

# axc_req <METHOD> <path|url> [json-body] [extra curl args...]  - prints "STATUS body".
axc_req() {
  local method="$1" target="$2" body="${3:-}" url
  shift 2
  [ "$#" -gt 0 ] && shift
  case "$target" in http*) url="$target" ;; *) url="$(axc_base_url)$target" ;; esac
  if [ -n "$body" ]; then
    curl -s -o "$AXC_DIR/last-body" -w '%{http_code}' -X "$method" "$url" \
      -H 'Content-Type: application/json' -d "$body" "$@"
  else
    curl -s -o "$AXC_DIR/last-body" -w '%{http_code}' -X "$method" "$url" "$@"
  fi
  printf ' %s\n' "$(head -c 400 "$AXC_DIR/last-body")"
}

# axc_sql "<sql>"  - run SQL against the store database, unaligned output, '|' separated.
axc_sql() {
  if [ -n "${AXC_PG_CONTAINER:-}" ]; then
    docker exec -i "$AXC_PG_CONTAINER" psql -X -q -At -U "${AXC_PG_USER:-postgres}" \
      -d "${AXC_PG_DB:-postgres}" -c "$1"
  elif [ -n "${AXC_PG_DSN:-}" ]; then
    psql -X -q -At "$AXC_PG_DSN" -c "$1"
  else
    echo "axc_sql: set AXC_PG_CONTAINER (+AXC_PG_DB/AXC_PG_USER) or AXC_PG_DSN" >&2
    return 1
  fi
}

# axc_tokens [--no-owner] [table]  - one line per processor and distinct token value.
# Handles the JPA token store (token is an oid large object) and the JDBC one (bytea).
# --no-owner drops the claim column so snapshots from different runs diff cleanly.
axc_tokens() {
  local owner=1 table expr cols mask=""
  if [ "${1:-}" = "--no-owner" ]; then owner=0; shift; fi
  table="${1:-token_entry}"
  cols="$(axc_sql "select column_name || ':' || data_type from information_schema.columns
                   where table_name = '$table'")" || return 1
  [ -z "$cols" ] && { echo "axc_tokens: table $table not found" >&2; return 1; }
  case "$cols" in
    *"token:oid"*) expr="convert_from(lo_get(token), 'UTF8')" ;;
    *"token:bytea"*) expr="convert_from(token, 'UTF8')" ;;
    *) expr="token::text" ;;
  esac
  case "$cols" in *"mask:"*) mask=", max(mask) as mask" ;; esac
  if [ "$owner" = 1 ]; then
    axc_sql "select processor_name, count(*) as segments$mask,
                    regexp_replace(token_type, '.*\.', '') as type, $expr as token,
                    case when bool_and(owner is not null) then 'claimed'
                         when bool_or(owner is not null) then 'partly-claimed' else 'unclaimed' end
             from $table group by processor_name, token_type, $expr order by 1, 4"
  else
    axc_sql "select processor_name, count(*) as segments$mask,
                    regexp_replace(token_type, '.*\.', '') as type, $expr as token
             from $table group by processor_name, token_type, $expr order by 1, 4"
  fi
}

# axc_snapshot <name> <sql>  - save query output to $AXC_DIR/<name>.txt.
axc_snapshot() {
  axc_sql "$2" > "$AXC_DIR/$1.txt" && echo "saved $AXC_DIR/$1.txt ($(wc -l < "$AXC_DIR/$1.txt" | tr -d ' ') lines)"
}

# axc_snapshot_tokens <name> [table]  - token snapshot without claim info.
axc_snapshot_tokens() {
  axc_tokens --no-owner "${2:-token_entry}" > "$AXC_DIR/$1.txt" && echo "saved $AXC_DIR/$1.txt"
}

# axc_schema <name>  - schema-only dump (Postgres), with per-run nonce lines removed.
axc_schema() {
  local out="$AXC_DIR/schema-$1.sql"
  if [ -n "${AXC_PG_CONTAINER:-}" ]; then
    docker exec -i "$AXC_PG_CONTAINER" pg_dump -s -U "${AXC_PG_USER:-postgres}" "${AXC_PG_DB:-postgres}"
  else
    pg_dump -s "$AXC_PG_DSN"
  fi | grep -v -E '^\\(restrict|unrestrict) |^-- Dumped (from|by)' > "$out"
  echo "saved $out"
}

# axc_diff <name-a> <name-b>  - diff two snapshots by name (or path); says IDENTICAL when equal.
axc_diff() {
  local a="$1" b="$2"
  [ -f "$a" ] || a="$AXC_DIR/$a"; [ -f "$a" ] || a="$a.txt"
  [ -f "$b" ] || b="$AXC_DIR/$b"; [ -f "$b" ] || b="$b.txt"
  if diff "$a" "$b"; then echo "IDENTICAL: $(basename "$a") vs $(basename "$b")"; else return 1; fi
}

# axc_logscan <logfile>  - WARN/ERROR lines with timestamps, pids, threads, ids and numbers
# normalized, counted. Works for app logs and test logs.
axc_logscan() {
  grep -E '(^|[[:space:]])(WARN|ERROR)([[:space:]]|$)' "$1" \
    | sed -E 's/^.*(WARN|ERROR)/\1/; s/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/UUID/g;
              s/\[[^]]*\]//g; s/[0-9]+/N/g; s/[[:space:]]+/ /g' \
    | cut -c1-220 | sort | uniq -c | sort -rn
}

# axc_logdiff <log-a> <log-b>  - WARN/ERROR kinds that differ between two logs.
axc_logdiff() {
  axc_logscan "$1" | sed -E 's/^ *[0-9]+ //' | sort -u > "$AXC_DIR/.logdiff-a"
  axc_logscan "$2" | sed -E 's/^ *[0-9]+ //' | sort -u > "$AXC_DIR/.logdiff-b"
  echo "only in $(basename "$1"):"; comm -23 "$AXC_DIR/.logdiff-a" "$AXC_DIR/.logdiff-b" | sed 's/^/  /'
  echo "only in $(basename "$2"):"; comm -13 "$AXC_DIR/.logdiff-a" "$AXC_DIR/.logdiff-b" | sed 's/^/  /'
}

# axc_axonserver_dev <container-name> [image]  - disposable dev-mode Axon Server on random
# host ports. Prints and exports AXC_AS_HTTP / AXC_AS_GRPC.
axc_axonserver_dev() {
  local name="$1" image="${2:-axoniq/axonserver:latest}"
  docker run -d --name "$name" -p 0:8024 -p 0:8124 \
    -e AXONIQ_AXONSERVER_DEVMODE_ENABLED=true -e AXONIQ_AXONSERVER_PLUGINS_ENABLED=false \
    "$image" > /dev/null || return 1
  AXC_AS_HTTP="$(docker port "$name" 8024 | head -1 | sed 's/.*://')"
  AXC_AS_GRPC="$(docker port "$name" 8124 | head -1 | sed 's/.*://')"
  export AXC_AS_HTTP AXC_AS_GRPC
  echo "axon server $name ($image): http=$AXC_AS_HTTP grpc=$AXC_AS_GRPC"
  docker image inspect "$image" --format 'image created {{.Created}}' 2>/dev/null
}

# axc_axonserver_init <http-port> [dcb: true|false]  - initialize a fresh node with a
# 'default' context (retries until the node accepts), then lists the contexts.
axc_axonserver_init() {
  local port="$1" dcb="${2:-false}" i=0
  while [ "$i" -lt 60 ]; do
    if curl -sf -X POST "http://localhost:$port/v2/cluster/init?dcb=$dcb" > /dev/null; then break; fi
    sleep 2
    i=$((i + 1))
  done
  sleep 5
  axc_axonserver_contexts "$port"
}

# axc_axonserver_contexts <http-port>  - context names and whether they are DCB contexts.
axc_axonserver_contexts() {
  curl -s "http://localhost:$1/v1/public/context" \
    | grep -o -E '"context":"[^"]*","dcbContext":(true|false)' \
    || echo "(no context list; check the server log, e.g. 'Creating DCB context')"
}
