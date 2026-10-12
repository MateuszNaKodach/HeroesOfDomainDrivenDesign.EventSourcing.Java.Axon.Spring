#!/usr/bin/env bash
# Write the resolved runtime dependencies of the project in the current directory to a
# sorted file of group:artifact:version lines, so two trees (baseline and updated) can be
# diffed with plain `diff`.
#
# Usage: dep-list.sh <output-file> [extra build-tool args...]
#   Maven:  uses ./mvnw (else mvn), dependency:list with scope runtime, all modules.
#   Gradle: uses ./gradlew (else gradle), dependencies of runtimeClasspath; set
#           DEP_LIST_GRADLE_PROJECT=:module to pick a subproject (default: root project).
set -euo pipefail

out="${1:?usage: dep-list.sh <output-file> [extra args...]}"
shift
case "$out" in /*) ;; *) out="$PWD/$out" ;; esac
raw="$(mktemp)"
trap 'rm -f "$raw"' EXIT

if [ -f pom.xml ]; then
  mvn_cmd=mvn
  [ -x ./mvnw ] && mvn_cmd=./mvnw
  "$mvn_cmd" -B -q dependency:list -DincludeScope=runtime -DoutputFile="$raw" -DappendOutput=true "$@" >/dev/null
  # Lines look like:   group:artifact:jar[:classifier]:version:scope [-- module x]
  sed -E 's/ -- .*$//; s/^[[:space:]]+//' "$raw" \
    | awk -F: 'NF>=5 { v=$(NF-1); print $1 ":" $2 ":" v }' \
    | sort -u > "$out"
elif [ -f build.gradle ] || [ -f build.gradle.kts ]; then
  gradle_cmd=gradle
  [ -x ./gradlew ] && gradle_cmd=./gradlew
  "$gradle_cmd" -q "${DEP_LIST_GRADLE_PROJECT:-}dependencies" --configuration runtimeClasspath "$@" > "$raw"
  # Lines look like:  +--- group:artifact:requested -> resolved (*)
  grep -E -- '--- ' "$raw" \
    | sed -E 's/^.*--- //; s/ \((\*|c|n)\)$//' \
    | awk '{
        split($1, p, ":");
        v = p[3];
        if ($2 == "->") v = $3;
        if (p[1] != "" && p[2] != "" && v != "") print p[1] ":" p[2] ":" v
      }' \
    | sort -u > "$out"
else
  echo "dep-list.sh: no pom.xml or build.gradle(.kts) in $PWD" >&2
  exit 1
fi

echo "wrote $(wc -l < "$out" | tr -d ' ') dependencies to $out"
