#!/usr/bin/env bash
#
# Mixed-version interop matrix for command results over Axon Server.
#
# Starts a throwaway Axon Server (dev mode) in Docker, builds one probe jar per framework variant, and lets
# dispatcher variants D send the probe commands to handler variants H through Axon Server:
#   - the full D x H matrix for HANDLERS x DISPATCHERS,
#   - plus one same-version pair (D = H) for every variant in SAME_VERSION_PAIRS.
# Every result is compared with the EXPECTED table below; the script exits non-zero on any mismatch.
#
# Works with the bash 3.2 that ships with macOS, and when invoked from zsh (./run-matrix.sh or bash run-matrix.sh).
#
# Environment:
#   AXONSERVER_IMAGE    Axon Server image (default axoniq/axonserver:2025.2.7)
#   SKIP_BUILD=true     reuse the jars under target/<variant>/ instead of building them
#   MVN                 Maven command (default: the repository's ./mvnw)
#   JAVA                java executable (default: java on PATH)
#   DISPATCH_TIMEOUT    seconds a dispatcher run may take (default 120)
#   HANDLERS            handler variants of the full matrix (default "v530snapshot v533 v540 v541fix");
#                       must contain v540
#   DISPATCHERS         dispatcher variants of the full matrix
#                       (default "v530snapshot v533 v540 v541fix v540workaround"); must contain v540
#   SAME_VERSION_PAIRS  variants that only run against themselves (default "v530 v531 v532")
#   REPORT_ONLY=true    skip building and running; re-check the results of the last run in target/matrix/
#                       against the current expectations

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
IMAGE="${AXONSERVER_IMAGE:-axoniq/axonserver:2025.2.7}"
MVN="${MVN:-$REPO_ROOT/mvnw}"
JAVA="${JAVA:-java}"
SKIP_BUILD="${SKIP_BUILD:-false}"
REPORT_ONLY="${REPORT_ONLY:-false}"
DISPATCH_TIMEOUT="${DISPATCH_TIMEOUT:-120}"

HANDLERS="${HANDLERS:-v530snapshot v533 v540 v541fix}"
DISPATCHERS="${DISPATCHERS:-v530snapshot v533 v540 v541fix v540workaround}"
SAME_VERSION_PAIRS="${SAME_VERSION_PAIRS-v530 v531 v532}"
# Every variant that is built: the union of all lists above, in first-seen order.
VARIANTS=""
for variant in $HANDLERS $DISPATCHERS $SAME_VERSION_PAIRS; do
    case " $VARIANTS " in
        *" $variant "*) ;;
        *) VARIANTS="${VARIANTS:+$VARIANTS }$variant" ;;
    esac
done
CASES="void value emptyString failure"

RUN_DIR="$SCRIPT_DIR/target/matrix"
LOG_DIR="$RUN_DIR/logs"
JVM_OPTS="-Daxoniq.update-checker.disabled=true"

CONTAINER_ID=""
MISMATCHES=""
HANDLER_PID=""
DISPATCHER_PID=""

# ---------------------------------------------------------------------------------------------------------------------
# EXPECTED table. The first version, written before the first run, is in the git history; README.md explains
# every change made after investigating a difference. A cell expectation is "OUTCOME|detail" and is matched as a
# bash glob pattern against the printed PROBE_RESULT line (without the "PROBE_RESULT|case|" prefix).
#
# The void case follows from two properties of a variant, both read from the framework sources:
#
# 1. What its handling side puts on the wire for a void command handler.
#    - No payload: 5.3.0, 5.3.1 and the local 5.3.0-SNAPSHOT. Their SimpleCommandBus completes a handler without
#      result with a null CommandResultMessage, and the connector sends a CommandResponse without payload for it.
#    - A payload typed java.lang.Void with empty data: 5.3.2 and later, including the dispatcher-only fix.
#      AxonFramework commit 4b971815a1 ("Ensure CommandResultMessage is never null", 5.3.2) makes SimpleCommandBus
#      return GenericCommandResultMessage(Void, null), which the connector writes with the type name and
#      Objects.requireNonNullElse(payload, new byte[0]).
# 2. Whether its dispatching side reads such a typed empty Void payload as "no result".
#    - No: every released connector and the snapshot only treat a payload without type as "no result", so they
#      hand byte[0] to resultAs(Void.class), which fails in Jackson.
#    - Yes: the dispatcher-only fix (empty type, or empty data with type java.lang.Void) and the application's
#      dispatch interceptor workaround (a java.lang.Void typed result with an empty byte[] becomes null).
# ---------------------------------------------------------------------------------------------------------------------
VOID_BUG='ERROR|org.axonframework.conversion.ConversionException: *byte\[\]* to *java.lang.Void*'

handler_sends_typed_empty_void() {
    case "$1" in
        v532 | v533 | v540 | v541fix) return 0 ;;
        *) return 1 ;;
    esac
}

dispatcher_reads_typed_empty_void_as_null() {
    case "$1" in
        v541fix | v540workaround) return 0 ;;
        *) return 1 ;;
    esac
}

expected() {
    local dispatcher="$1" handler="$2" case_name="$3"
    case "$case_name" in
        void)
            if handler_sends_typed_empty_void "$handler" && ! dispatcher_reads_typed_empty_void_as_null "$dispatcher"; then
                echo "$VOID_BUG"
            else
                echo 'OK|null'
            fi
            ;;
        value)
            echo 'OK|"pong"'
            ;;
        emptyString)
            # The Jackson converter turns the String "" into an empty byte[] (StringToByteArrayConverter), so it
            # travels as a payload typed java.lang.String with empty data. Every connector, including the fix,
            # keeps it a String, and so does the workaround since it only touches java.lang.Void typed results.
            # (The first, unnarrowed workaround returned null here; see README.md.)
            echo 'OK|""'
            ;;
        failure)
            # Every pair must propagate the failure exactly like the same-version pair of 5.4.0, which itself must
            # carry the original message.
            echo "SAME_AS_BASELINE"
            ;;
    esac
}
FAILURE_BASELINE_DISPATCHER="v540"
FAILURE_BASELINE_HANDLER="v540"
FAILURE_BASELINE_PATTERN='ERROR|*probe failure*'

# ---------------------------------------------------------------------------------------------------------------------

log() {
    echo "[run-matrix] $*" >&2
}

cleanup() {
    local exit_code=$?
    if [ -n "$DISPATCHER_PID" ] && kill -0 "$DISPATCHER_PID" 2>/dev/null; then
        kill "$DISPATCHER_PID" 2>/dev/null || true
    fi
    if [ -n "$HANDLER_PID" ] && kill -0 "$HANDLER_PID" 2>/dev/null; then
        kill "$HANDLER_PID" 2>/dev/null || true
        wait "$HANDLER_PID" 2>/dev/null || true
    fi
    if [ -n "$CONTAINER_ID" ]; then
        log "removing Axon Server container ${CONTAINER_ID:0:12}"
        docker rm -f "$CONTAINER_ID" >/dev/null 2>&1 || true
    fi
    exit "$exit_code"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

host_port() {
    docker port "$CONTAINER_ID" "$1/tcp" | head -1 | sed 's/.*://'
}

wait_for() {
    local description="$1" attempts="$2"
    shift 2
    local i
    for i in $(seq 1 "$attempts"); do
        if "$@"; then
            return 0
        fi
        sleep 1
    done
    log "timed out waiting for $description"
    return 1
}

axon_server_healthy() {
    [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$HTTP_PORT/actuator/health")" = "200" ]
}

default_context_exists() {
    curl -s "http://localhost:$HTTP_PORT/v1/public/context" | grep -q '"context":"default"'
}

registered_command_count() {
    curl -s "http://localhost:$HTTP_PORT/v1/public/command-metrics?context=default" \
        | grep -o "\"clientId\":\"probe-handler-$1-[^\"]*\"" | wc -l | tr -d ' '
}

handler_registered() {
    [ "$(registered_command_count "$1")" -ge 4 ]
}

handler_unregistered() {
    [ "$(registered_command_count "$1")" -eq 0 ]
}

build_variants() {
    local variant
    for variant in $VARIANTS; do
        log "building variant $variant"
        "$MVN" -q -nsu -f "$SCRIPT_DIR/pom.xml" -P"$variant" package -DskipTests
    done
}

jar_of() {
    echo "$SCRIPT_DIR/target/$1/command-interop-probe-$1.jar"
}

start_axon_server() {
    log "starting Axon Server ($IMAGE)"
    CONTAINER_ID="$(docker run -d --rm -p 0:8024 -p 0:8124 \
        -e AXONIQ_AXONSERVER_DEVMODE_ENABLED=true \
        "$IMAGE")"
    HTTP_PORT="$(host_port 8024)"
    GRPC_PORT="$(host_port 8124)"
    log "Axon Server container ${CONTAINER_ID:0:12}: http=$HTTP_PORT grpc=$GRPC_PORT"
    wait_for "Axon Server health" 120 axon_server_healthy
    curl -s -X POST "http://localhost:$HTTP_PORT/v2/cluster/init" \
        -H 'Content-Type: application/json' -d '{}' >/dev/null
    wait_for "the default context" 60 default_context_exists
}

start_handler() {
    local variant="$1"
    log "starting handler $variant"
    "$JAVA" $JVM_OPTS -jar "$(jar_of "$variant")" \
        --probe.role=handler \
        --axon.axonserver.servers="localhost:$GRPC_PORT" \
        --spring.application.name="probe-handler-$variant" \
        --axon.axonserver.component-name="probe-handler-$variant" \
        --axon.axonserver.client-id="probe-handler-$variant" \
        >"$LOG_DIR/handler-$variant.log" 2>&1 &
    HANDLER_PID=$!
    wait_for "handler $variant to register its command handlers" 120 handler_registered "$variant"
}

stop_handler() {
    local variant="$1"
    log "stopping handler $variant"
    kill "$HANDLER_PID" 2>/dev/null || true
    wait "$HANDLER_PID" 2>/dev/null || true
    HANDLER_PID=""
    wait_for "handler $variant to unregister" 60 handler_unregistered "$variant"
}

# Runs one dispatcher against the running handler and stores its PROBE_* lines in $RUN_DIR/<D>-<H>.txt.
run_dispatcher() {
    local dispatcher="$1" handler="$2"
    local log_file="$LOG_DIR/dispatcher-$dispatcher-to-$handler.log"
    log "dispatching from $dispatcher to $handler"
    "$JAVA" $JVM_OPTS -jar "$(jar_of "$dispatcher")" \
        --probe.role=dispatcher \
        --axon.axonserver.servers="localhost:$GRPC_PORT" \
        --spring.application.name="probe-dispatcher-$dispatcher" \
        --axon.axonserver.component-name="probe-dispatcher-$dispatcher" \
        --axon.axonserver.client-id="probe-dispatcher-$dispatcher-to-$handler" \
        >"$log_file" 2>&1 &
    DISPATCHER_PID=$!
    local pid=$DISPATCHER_PID waited=0
    while kill -0 "$pid" 2>/dev/null; do
        if [ "$waited" -ge "$DISPATCH_TIMEOUT" ]; then
            log "dispatcher $dispatcher timed out, killing it"
            kill "$pid" 2>/dev/null || true
            break
        fi
        sleep 1
        waited=$((waited + 1))
    done
    wait "$pid" 2>/dev/null || true
    DISPATCHER_PID=""
    grep '^PROBE_' "$log_file" >"$RUN_DIR/$dispatcher-$handler.txt" || true
}

# Prints "OUTCOME|detail" of one case, or "MISSING|no result printed" when the dispatcher printed nothing for it.
actual() {
    local dispatcher="$1" handler="$2" case_name="$3" line
    line="$(grep "^PROBE_RESULT|$case_name|" "$RUN_DIR/$dispatcher-$handler.txt" 2>/dev/null | head -1 || true)"
    if [ -z "$line" ]; then
        echo "MISSING|no result printed"
    else
        echo "${line#PROBE_RESULT|$case_name|}"
    fi
}

# Returns 0 when the actual result of a cell matches its expectation.
cell_matches() {
    local dispatcher="$1" handler="$2" case_name="$3" want got
    want="$(expected "$dispatcher" "$handler" "$case_name")"
    got="$(actual "$dispatcher" "$handler" "$case_name")"
    if [ "$want" = "SAME_AS_BASELINE" ]; then
        local baseline
        baseline="$(actual "$FAILURE_BASELINE_DISPATCHER" "$FAILURE_BASELINE_HANDLER" "$case_name")"
        # shellcheck disable=SC2053
        [[ $baseline == $FAILURE_BASELINE_PATTERN ]] && [ "$got" = "$baseline" ]
    else
        # shellcheck disable=SC2053
        [[ $got == $want ]]
    fi
}

describe_expected() {
    local want
    want="$(expected "$1" "$2" "$3")"
    if [ "$want" = "SAME_AS_BASELINE" ]; then
        echo "same as $FAILURE_BASELINE_DISPATCHER->$FAILURE_BASELINE_HANDLER: $(actual "$FAILURE_BASELINE_DISPATCHER" "$FAILURE_BASELINE_HANDLER" "$3") (and matching $FAILURE_BASELINE_PATTERN)"
    else
        echo "$want"
    fi
}

short_cell() {
    local got="$1" outcome detail
    outcome="${got%%|*}"
    detail="${got#*|}"
    case "$outcome" in
        OK) echo "OK $detail" ;;
        ERROR)
            local class="${detail%%:*}"
            echo "ERROR ${class##*.}"
            ;;
        *) echo "$outcome" ;;
    esac
}

# Prints one cell, marking and counting it when it does not match its expectation.
print_cell() {
    local dispatcher="$1" handler="$2" case_name="$3" cell
    cell="$(short_cell "$(actual "$dispatcher" "$handler" "$case_name")")"
    if cell_matches "$dispatcher" "$handler" "$case_name"; then
        printf '| %-34s' "$cell"
    else
        printf '| %-34s' "$cell [MISMATCH]"
        MISMATCHES="$MISMATCHES $dispatcher:$handler:$case_name"
    fi
}

print_matrix() {
    local case_name dispatcher handler variant mismatch
    MISMATCHES=""
    echo
    echo "Command result interop matrix (rows: dispatcher D, columns: handler H)"
    echo "Axon Server image: $IMAGE"
    for case_name in $CASES; do
        echo
        echo "case: $case_name"
        printf '  %-16s' "D \\ H"
        for handler in $HANDLERS; do
            printf '| %-34s' "$handler"
        done
        echo
        for dispatcher in $DISPATCHERS; do
            printf '  %-16s' "$dispatcher"
            for handler in $HANDLERS; do
                print_cell "$dispatcher" "$handler" "$case_name"
            done
            echo
        done
    done

    if [ -n "$SAME_VERSION_PAIRS" ]; then
        echo
        echo "Same-version pairs (D = H)"
        printf '  %-16s' "variant"
        for case_name in $CASES; do
            printf '| %-34s' "$case_name"
        done
        echo
        for variant in $SAME_VERSION_PAIRS; do
            printf '  %-16s' "$variant"
            for case_name in $CASES; do
                print_cell "$variant" "$variant" "$case_name"
            done
            echo
        done
    fi

    echo
    echo "Variants (from the PROBE_VARIANT banner of each node):"
    for variant in $VARIANTS; do
        grep -h '^PROBE_VARIANT' "$LOG_DIR"/handler-"$variant".log "$RUN_DIR"/"$variant"-*.txt 2>/dev/null \
            | sort -u | sed 's/^/  /' || true
    done

    if [ -n "$MISMATCHES" ]; then
        echo
        echo "Mismatches ($(echo $MISMATCHES | wc -w | tr -d ' ')):"
        for mismatch in $MISMATCHES; do
            dispatcher="${mismatch%%:*}"
            handler="${mismatch#*:}"
            handler="${handler%%:*}"
            case_name="${mismatch##*:}"
            echo "  $case_name D=$dispatcher H=$handler"
            echo "    expected: $(describe_expected "$dispatcher" "$handler" "$case_name")"
            echo "    actual:   $(actual "$dispatcher" "$handler" "$case_name")"
        done
        echo
        echo "RESULT: FAILED ($(echo $MISMATCHES | wc -w | tr -d ' ') mismatching cells)"
        return 1
    fi
    echo
    echo "RESULT: PASSED (all cells match the expectations)"
}

main() {
    case " $HANDLERS " in *" $FAILURE_BASELINE_HANDLER "*) ;; *) log "HANDLERS must contain $FAILURE_BASELINE_HANDLER"; exit 2 ;; esac
    case " $DISPATCHERS " in *" $FAILURE_BASELINE_DISPATCHER "*) ;; *) log "DISPATCHERS must contain $FAILURE_BASELINE_DISPATCHER"; exit 2 ;; esac
    if [ "$REPORT_ONLY" = "true" ]; then
        print_matrix
        return
    fi
    rm -rf "$RUN_DIR"
    mkdir -p "$LOG_DIR"
    if [ "$SKIP_BUILD" != "true" ]; then
        build_variants
    fi
    local variant
    for variant in $VARIANTS; do
        [ -f "$(jar_of "$variant")" ] || { log "missing jar for $variant, run without SKIP_BUILD"; exit 2; }
    done

    start_axon_server

    local handler dispatcher
    for handler in $HANDLERS; do
        start_handler "$handler"
        for dispatcher in $DISPATCHERS; do
            run_dispatcher "$dispatcher" "$handler"
        done
        stop_handler "$handler"
    done

    local variant_pair
    for variant_pair in $SAME_VERSION_PAIRS; do
        start_handler "$variant_pair"
        run_dispatcher "$variant_pair" "$variant_pair"
        stop_handler "$variant_pair"
    done

    print_matrix
}

main "$@"
