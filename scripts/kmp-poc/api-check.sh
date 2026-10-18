#!/usr/bin/env bash
# Prints every baseline API line missing from the current adapty-ui release API, then runs consumer-refs-check.sh.
# Scope: classes outside com.adapty.ui.internal.* (baseline filtered once with the same rule as api-dump.sh).
set -euo pipefail
cd "$(dirname "$0")/../.."

current=$(mktemp)
trap 'rm -f "$current"' EXIT
scripts/kmp-poc/api-dump.sh > "$current"

status=0
removed=$(comm -23 <(LC_ALL=C sort -u scripts/kmp-poc/api-baseline.txt) <(LC_ALL=C sort -u "$current"))
if [ -z "$removed" ]; then
  echo "API OK (0 removed)"
else
  printf '%s\n' "$removed"
  echo "API BROKEN ($(printf '%s\n' "$removed" | wc -l | tr -d ' ') removed)"
  status=1
fi

scripts/kmp-poc/consumer-refs-check.sh || status=1
exit $status
