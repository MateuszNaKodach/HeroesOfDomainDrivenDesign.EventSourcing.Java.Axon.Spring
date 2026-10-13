#!/usr/bin/env bash
#
# Generates jpa-event-store.sql: a Postgres dump (schema, rows and large objects) written by the app built on
# Axon Framework 5.3.0-SNAPSHOT, used by JpaEventStoreDataFromAxon530CompatibilityTest ("old data, new version").
#
# Usage: generate-fixture.sh <path-to-pre-upgrade-app.jar>
#
# The jar must be the app as it was before the Axon 5.4.0 upgrade, e.g. built from commit ec132e9:
#   git worktree add --detach /tmp/heroes-old ec132e9
#   (cd /tmp/heroes-old && ./mvnw -nsu -q -DskipTests package)   # the 5.3.0-SNAPSHOT jars are local, never use -U
#   generate-fixture.sh /tmp/heroes-old/target/heroesofddd-0.0.1-SNAPSHOT.jar
#   git worktree remove --force /tmp/heroes-old
#
# Steps: start a throwaway postgres container on a random port, run the old jar against it (default JPA profile,
# no Axon Server), run a fixed scenario through the REST API, wait until the event processors caught up, stop the
# app gracefully (so it releases its token claims), pg_dump everything into jpa-event-store.sql, remove the
# container.
#
# Scenario (game "compat-game", player "player-1"):
#   1. build dwelling-1 (angel, 3000 gold + 1 gem per troop) and dwelling-2 (pegasus, 1000 gold per troop)
#   2. increase available creatures of both dwellings by 5
#   3. recruit 3 angels from dwelling-1 and 2 pegasi from dwelling-2 into army-1
#   The WhenCreatureRecruitedThenAddToArmy automation then appends two CreatureAddedToArmy events to army-1.
#
# Requirements: docker, java (the version the jar was built for), curl.

set -euo pipefail

JAR="${1:?usage: generate-fixture.sh <path-to-pre-upgrade-app.jar>}"
JAR="$(cd "$(dirname "$JAR")" && pwd)/$(basename "$JAR")"
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/jpa-event-store.sql"

# Keep in sync with JpaEventStoreDataFromAxon530CompatibilityTest.
POSTGRES_IMAGE="postgres:16.13"
DB_NAME="heroes"
DB_USER="heroes"
DB_PASSWORD="heroes"

GAME_ID="compat-game"
PLAYER_ID="player-1"

WORK="$(mktemp -d)"
CID=""
APP_PID=""

cleanup() {
  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then kill "$APP_PID" || true; fi
  if [[ -n "$CID" ]]; then docker rm -f "$CID" >/dev/null || true; fi
  echo "app logs kept in $WORK"
}
trap cleanup EXIT

psql_q() { docker exec -i "$CID" psql -U "$DB_USER" -d "$DB_NAME" -At -c "$1"; }

echo "== starting $POSTGRES_IMAGE"
CID="$(docker run -d -e POSTGRES_DB="$DB_NAME" -e POSTGRES_USER="$DB_USER" -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -p 127.0.0.1::5432 "$POSTGRES_IMAGE")"
# TCP readiness: during initdb the temporary server only listens on the unix socket.
for _ in $(seq 1 60); do
  docker exec "$CID" pg_isready -q -h 127.0.0.1 -U "$DB_USER" -d "$DB_NAME" && break
  sleep 1
done
PG_PORT="$(docker port "$CID" 5432/tcp | head -1 | sed 's/.*://')"
echo "postgres on port $PG_PORT"

echo "== starting $(basename "$JAR")"
SERVER_PORT=0 \
SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:$PG_PORT/$DB_NAME" \
SPRING_DATASOURCE_USERNAME="$DB_USER" \
SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
SPRING_JPA_SHOW_SQL=false \
LOGGING_FILE_NAME="$WORK/app.log" \
  java -jar "$JAR" > "$WORK/app.stdout" 2>&1 &
APP_PID=$!

APP_PORT=""
for _ in $(seq 1 120); do
  APP_PORT="$(sed -n 's/.*Tomcat started on port \([0-9]*\).*/\1/p' "$WORK/app.stdout" | head -1)"
  if [[ -n "$APP_PORT" ]] && curl -sf "http://127.0.0.1:$APP_PORT/actuator/health" >/dev/null; then break; fi
  kill -0 "$APP_PID" 2>/dev/null || { echo "app died"; tail -50 "$WORK/app.stdout"; exit 1; }
  sleep 1
done
curl -sf "http://127.0.0.1:$APP_PORT/actuator/health" >/dev/null || { echo "app not healthy"; exit 1; }
echo "app up on port $APP_PORT"

BASE="http://127.0.0.1:$APP_PORT/games/$GAME_ID"
put() {
  echo "PUT $1"
  curl -sf -X PUT "$BASE$1" -H 'Content-Type: application/json' -H "X-Player-Id: $PLAYER_ID" -d "$2" >/dev/null
}

echo "== scenario"
put /dwellings/dwelling-1 '{"creatureId":"angel","costPerTroop":{"gold":3000,"gems":1}}'
put /dwellings/dwelling-2 '{"creatureId":"pegasus","costPerTroop":{"gold":1000}}'
put /dwellings/dwelling-1/available-creatures-increases '{"creatureId":"angel","increaseBy":5}'
put /dwellings/dwelling-2/available-creatures-increases '{"creatureId":"pegasus","increaseBy":5}'
put /dwellings/dwelling-1/creature-recruitments \
  '{"creatureId":"angel","armyId":"army-1","quantity":3,"expectedCost":{"gold":9000,"gems":3}}'
put /dwellings/dwelling-2/creature-recruitments \
  '{"creatureId":"pegasus","armyId":"army-1","quantity":2,"expectedCost":{"gold":2000}}'

dump_state() {
  psql_q "select global_index, aggregate_identifier, aggregate_sequence_number, type from aggregate_event_entry order by global_index"
  psql_q "select processor_name, segment, token_type, convert_from(lo_get(token), 'UTF8') from token_entry order by 1, 2"
}

echo "== waiting for the automation and the read model to catch up"
# Caught up = both CreatureAddedToArmy events are stored and no token moved during the last 3 seconds.
# (Token positions are not compared with global_index on purpose: their numbering is an engine detail.)
tokens_snapshot() { psql_q "select processor_name, segment, convert_from(lo_get(token), 'UTF8') from token_entry order by 1, 2"; }
added_to_army() { psql_q "select count(*) from aggregate_event_entry where type like '%.CreatureAddedToArmy'"; }
previous=""
caught_up=false
for _ in $(seq 1 30); do
  current="$(tokens_snapshot)"
  if [[ "$(added_to_army)" == "2" && "$current" == "$previous" ]]; then caught_up=true; break; fi
  previous="$current"
  sleep 3
done
$caught_up || { echo "processors did not catch up"; dump_state; exit 1; }
# Stop gracefully so token claims are released.
kill "$APP_PID"
while kill -0 "$APP_PID" 2>/dev/null; do sleep 1; done
APP_PID=""

echo "== state written by the old version"
dump_state

AXON_VERSION="$(unzip -p "$JAR" BOOT-INF/classpath.idx | sed -n 's/.*axon-messaging-\(.*\)\.jar.*/\1/p')"
echo "== dumping (axon-messaging $AXON_VERSION)"
{
  echo "-- Fixture for JpaEventStoreDataFromAxon530CompatibilityTest. Do not edit, regenerate with generate-fixture.sh."
  echo "-- Written by $(basename "$JAR") (axon-messaging $AXON_VERSION), JPA event store, on $POSTGRES_IMAGE."
  echo "-- Scenario: see generate-fixture.sh."
  # -b: include large objects; token_entry.token and the event payload/metadata columns are oids.
  docker exec "$CID" pg_dump -U "$DB_USER" -d "$DB_NAME" -b --no-owner --no-privileges
} > "$OUT"
echo "written $OUT ($(wc -c < "$OUT") bytes)"
