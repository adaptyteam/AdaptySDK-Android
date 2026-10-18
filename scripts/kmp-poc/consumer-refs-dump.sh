#!/usr/bin/env bash
# Lists every constant-pool reference to com/adapty/ui/internal/ classes/members in the given AARs/jars.
# Refs to classes defined in the inputs themselves are skipped.
# Output: "Owner" for class refs, "Owner.name:descriptor" for method/field refs; sorted unique.
# Usage: consumer-refs-dump.sh <aar-or-jar>... (default: crossplatform + adapty-ui-video release AARs)
set -euo pipefail
cd "$(dirname "$0")/../.."

if [ $# -eq 0 ]; then
  set -- crossplatform/build/outputs/aar/crossplatform-release.aar adapty-ui-video/build/outputs/aar/adapty-ui-video-release.aar
fi

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
i=0
for f in "$@"; do
  [ -f "$f" ] || { echo "not found: $f" >&2; exit 1; }
  i=$((i + 1)); d="$work/$i"; mkdir -p "$d/classes"
  case "$f" in
    *.aar) unzip -q -o "$f" classes.jar -d "$d"; jar="$d/classes.jar" ;;
    *) jar="$f" ;;
  esac
  (cd "$d/classes" && unzip -q -o "$jar")
done

# Classes defined by the consumers themselves (adapty-ui-video ships its own internal-package classes).
for d in "$work"/*/classes; do (cd "$d" && find . -name '*.class' | sed 's|^\./||; s|\.class$||'); done | sort -u > "$work/own"

# javap -v prints the constant pool; resolved refs appear as "// Owner.name:desc" or "// Owner".
find "$work" -name '*.class' -print0 | xargs -0 javap -v -p 2>/dev/null | awk '
  FNR == NR { own[$0] = 1; next }
  /^ *#[0-9]+ = (Class|Methodref|InterfaceMethodref|Fieldref) / {
    ref = $0; sub(/.*\/\/ /, "", ref); gsub(/"/, "", ref)
    if ($3 == "Class") { sub(/^\[+L?/, "", ref); sub(/;$/, "", ref) }
    owner = ref; sub(/\..*/, "", owner)
    if (index(ref, "com/adapty/ui/internal/") == 1 && !(owner in own)) print ref
  }
' "$work/own" - | LC_ALL=C sort -u
