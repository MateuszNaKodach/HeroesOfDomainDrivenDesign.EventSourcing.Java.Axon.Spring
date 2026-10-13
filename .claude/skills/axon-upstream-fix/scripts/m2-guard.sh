#!/usr/bin/env bash
# Guard released artifacts in the local Maven repository around a local install of a fix.
#
#   m2-guard.sh snapshot <groupId> <artifactId> <version>   record checksums and mtimes
#   m2-guard.sh verify   <groupId> <artifactId> <version>   compare, print OK or the differences
#
# Run "snapshot" before installing the fixed module under a -SNAPSHOT version and "verify" after.
# A difference means the released version directory was overwritten (for example the version
# override did not apply) and must be restored before anything else is built against it.
#
# Env: M2_REPO (default ~/.m2/repository), M2_GUARD_DIR (default ${TMPDIR:-/tmp}/m2-guard).
set -euo pipefail

usage() {
  echo "usage: $0 snapshot|verify <groupId> <artifactId> <version>" >&2
  exit 2
}

[ $# -eq 4 ] || usage
mode=$1 group=$2 artifact=$3 version=$4

repo=${M2_REPO:-$HOME/.m2/repository}
dir="$repo/${group//.//}/$artifact/$version"
guard_dir=${M2_GUARD_DIR:-${TMPDIR:-/tmp}/m2-guard}
state="$guard_dir/$group-$artifact-$version.txt"

hash_file() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1
  else shasum -a 256 "$1" | cut -d' ' -f1
  fi
}

mtime() {
  stat -f %m "$1" 2>/dev/null || stat -c %Y "$1"
}

record() {
  find "$dir" -type f ! -name '*.lastUpdated' ! -name '_remote.repositories' ! -name 'resolver-status.properties' \
    | LC_ALL=C sort \
    | while IFS= read -r f; do
        printf '%s %s %s\n' "$(hash_file "$f")" "$(mtime "$f")" "${f#"$dir"/}"
      done
}

if [ ! -d "$dir" ]; then
  echo "no released version directory at $dir (nothing to guard; resolve it first if the app uses it)" >&2
  exit 1
fi

case "$mode" in
  snapshot)
    mkdir -p "$guard_dir"
    record > "$state"
    echo "recorded $(wc -l < "$state" | tr -d ' ') files of $group:$artifact:$version in $state"
    ;;
  verify)
    [ -f "$state" ] || { echo "no snapshot at $state; run '$0 snapshot $group $artifact $version' first" >&2; exit 1; }
    if diff <(cat "$state") <(record) > "$state.diff"; then
      echo "OK: $group:$artifact:$version unchanged"
    else
      echo "CHANGED: $group:$artifact:$version differs from the snapshot (< before, > now):"
      cat "$state.diff"
      exit 1
    fi
    ;;
  *) usage ;;
esac
