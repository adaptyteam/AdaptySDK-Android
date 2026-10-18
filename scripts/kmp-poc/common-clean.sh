#!/usr/bin/env bash
# Greps adapty-ui commonMain for core-SDK / Android / JVM imports and FQ uses (Task 6c+: shared code lives in commonMain).
# Prints `clean` or the offending lines (file:line:text). Exit code 1 when offenders exist.
set -euo pipefail
cd "$(dirname "$0")/../.."

root=adapty-ui/src/commonMain/kotlin/com/adapty/ui

import_re='^import (android\.|androidx\.(activity|core)\.|java\.|javax\.|com\.google\.gson|com\.adapty\.(models|errors|internal|utils|Adapty)|com\.adapty\.ui\.R$)'
# FQ uses outside import/package lines: android./java. (definition) plus androidx.activity/core, core SDK and Android R (stricter)
fq_re='(^|[^A-Za-z0-9_.])((android|java)\.[a-z]+\.|androidx\.(activity|core)\.|com\.adapty\.(models|errors|internal|utils)\.|com\.adapty\.Adapty\b|com\.adapty\.ui\.R\b)'

out=""
while IFS= read -r f; do
  rel=${f#"$root"/}
  hits=$(grep -nE "$import_re" "$f" || true)
  fq=$(grep -nE "$fq_re" "$f" | grep -vE '^[0-9]+:(import|package) ' | grep -vE '^[0-9]+:\s*(//|\*|/\*)' || true)
  for h in "$hits" "$fq"; do
    [ -n "$h" ] && out+=$(printf '%s\n' "$h" | sed "s|^|$rel:|")$'\n'
  done
done < <(find "$root" -name '*.kt' | LC_ALL=C sort)

if [ -z "$out" ]; then
  echo clean
else
  printf '%s' "$out"
  echo "offenders: $(printf '%s' "$out" | grep -c '') lines in $(printf '%s' "$out" | cut -d: -f1 | sort -u | grep -c '') files"
  exit 1
fi
