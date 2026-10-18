#!/usr/bin/env bash
# Verifies every ref in consumer-refs-baseline.txt still exists (same name + descriptor) in the
# current adapty-ui release classes.jar. Member lookup walks in-jar superclasses/interfaces.
set -euo pipefail
cd "$(dirname "$0")/../.."

aar=adapty-ui/build/outputs/aar/adapty-ui-release.aar
[ -f "$aar" ] || { echo "release AAR not found; run ./gradlew :adapty-ui:assembleRelease" >&2; exit 1; }
baseline=scripts/kmp-poc/consumer-refs-baseline.txt

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
unzip -q -o "$aar" classes.jar -d "$work"
mkdir -p "$work/classes"
(cd "$work/classes" && unzip -q -o ../classes.jar)

# Index: "C <class>", "S <class> <super>", "M <class> <name>:<descriptor>" (slash-separated names).
index_javap() {
  awk '
    /^Compiled from/ { next }
    /^[^ }].*\{$/ {
      hdr = $0; sub(/ *\{$/, "", hdr)
      match(hdr, /(class|interface|enum) [^ <]+/); cls = substr(hdr, RSTART, RLENGTH); sub(/^[a-z]+ /, "", cls)
      gsub(/\./, "/", cls);
      print "C " cls
      # Drop generic args, then collect supertypes after extends/implements.
      h = hdr; while (gsub(/<[^<>]*>/, "", h)) {}
      n = split(h, t, /[ ,]+/); on = 0
      for (k = 1; k <= n; k++) {
        if (t[k] == "permits") break
        if (t[k] == "extends" || t[k] == "implements") { on = 1; continue }
        if (on && t[k] != "") { s = t[k]; gsub(/\./, "/", s); print "S " cls " " s }
      }
      next
    }
    /^}/ { cls = ""; next }
    cls != "" && /^  [^ ]/ {
      decl = $0; sub(/^ +/, "", decl)
      if (decl ~ /^static \{\};/) { name = "<clinit>"; next_is_desc = 1; next }
      p = index(decl, "(")
      if (p > 0) { pre = substr(decl, 1, p - 1) } else { pre = decl; sub(/;$/, "", pre); sub(/ = .*/, "", pre) }
      n = split(pre, t, / /); name = t[n]
      gsub(/\./, "/", name)
      if (name == cls) name = "<init>"
      sub(/.*\//, "", name)
      next_is_desc = 1; next
    }
    next_is_desc && /descriptor:/ { d = $0; sub(/.*descriptor: /, "", d); print "M " cls " " name ":" d; next_is_desc = 0 }
  '
}
(cd "$work/classes" && find . -name '*.class' | sed 's|^\./||; s|\.class$||' | xargs javap -s -p -cp . 2>/dev/null) \
  | index_javap > "$work/index"
# Resolve inherited members declared in JDK supertypes (e.g. Enum.ordinal); other external supers stay unresolved.
for _ in 1 2 3; do
  ext=$(awk '$1 == "C" { c[$2] = 1 } $1 == "S" { s[$3] = 1 } END { for (k in s) if (!(k in c) && k ~ /^java\//) print k }' "$work/index" | tr '/' '.')
  [ -n "$ext" ] || break
  printf '%s\n' $ext | xargs javap -s -p 2>/dev/null | index_javap >> "$work/index"
done

awk '
  FNR == NR {
    if ($1 == "C") cls[$2] = 1
    else if ($1 == "S") sup[$2] = sup[$2] " " $3
    else if ($1 == "M") mem[$2 " " $3] = 1
    next
  }
  function has(c, m,   n, k, t) {
    if (!(c in cls)) return 0
    if ((c " " m) in mem) return 1
    n = split(sup[c], t, " ")
    for (k = 1; k <= n; k++) if (has(t[k], m)) return 1
    return 0
  }
  NF == 0 { next }
  {
    total++
    ref = $0; dot = index(ref, ".")
    if (dot == 0) { ok = (ref in cls) }
    else { ok = has(substr(ref, 1, dot - 1), substr(ref, dot + 1)) }
    if (!ok) { print "MISSING " ref; missing++ }
  }
  END {
    if (missing) { print "CONSUMER-REFS BROKEN (" missing " of " total " missing)"; exit 1 }
    print "CONSUMER-REFS OK (" total " refs)"
  }
' "$work/index" "$baseline"
