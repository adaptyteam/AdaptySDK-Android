#!/usr/bin/env bash
# Dumps the public JVM API of the adapty-ui release AAR (javap -public), filtered and sorted.
# Scope: classes outside com.adapty.ui.internal.* (internal symbols are covered by consumer-refs-check.sh).
# Usage: api-dump.sh [path/to/adapty-ui-release.aar]  (default: this tree's release AAR)
set -euo pipefail
[ $# -gt 0 ] && aar=$(cd "$(dirname "$1")" && pwd)/$(basename "$1") || aar=
cd "$(dirname "$0")/../.."

[ -n "$aar" ] || aar=$(ls adapty-ui/build/outputs/aar/*-release.aar 2>/dev/null | head -n1 || true)
[ -f "$aar" ] || { echo "release AAR not found; run ./gradlew :adapty-ui:assembleRelease" >&2; exit 1; }

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
unzip -q -o "$aar" classes.jar -d "$work"
mkdir -p "$work/classes"
(cd "$work/classes" && unzip -q -o ../classes.jar)

classes=$(cd "$work/classes" && find . -name '*.class' | sed 's|^\./||; s|\.class$||' \
  | grep -Ev '\$[0-9]|\$lambda|ComposableSingletons|WhenMappings' | sort)

# One batched javap call; each class block is "<header> {", indented members, "}".
# Keep only public/protected class headers. Drop a member only if its NAME contains '$'
# (mangled internals, synthetic accessors), except '*$default' bridges; '$' in types is fine.
(cd "$work/classes" && printf '%s\n' $classes | xargs javap -public -cp . 2>/dev/null) | awk '
  /^Compiled from/ { next }
  /^[^ }].*\{$/ {
    hdr = $0; sub(/ *\{$/, "", hdr)
    keep = (hdr ~ /^(public|protected) /)
    if (keep) {
      match(hdr, /(class|interface|enum) [^ <]+/); name = substr(hdr, RSTART, RLENGTH); sub(/^[a-z]+ /, "", name)
      print name ": " hdr
    }
    next
  }
  /^}/ { keep = 0; next }
  keep && /^ / {
    m = $0; sub(/;[ \t]*$/, "", m)
    if (index(m, "(")) { m = substr(m, 1, index(m, "(") - 1) }
    n = split(m, t, " "); mname = t[n]
    if (mname != name && mname ~ /\$/ && mname !~ /\$default$/) next
    print name ":" $0
  }
' | { grep -Ev '^com\.adapty\.ui\.internal\.' || true; } | LC_ALL=C sort -u

n=$(javap -p -cp "$work/classes" 'com.adapty.ui.AdaptyUI$FlowConfiguration' 2>/dev/null | grep -o '\$adapty_ui_release' | wc -l | tr -d ' ')
echo "mangled-internal: $n" >&2
