#!/usr/bin/env bash
# Greps adapty-ui androidMain files destined for commonMain for core-SDK / Android / JVM imports and FQ uses.
# Prints `clean` or the offending lines (file:line:text). Exit code 1 when offenders exist.
set -euo pipefail
cd "$(dirname "$0")/../.."

root=adapty-ui/src/androidMain/kotlin/com/adapty/ui

# Files that stay in androidMain (paths relative to $root; globs allowed)
stay_android=(
  'AdaptyFlowView.kt'
  'onboardings/*'
  'internal/AdaptyUIAndroid.kt'
  'internal/platform/*'
  'internal/cache/CacheFileManager.kt'
  'internal/cache/MediaDownloader.kt'
  'internal/cache/MediaSaver.kt'
  'internal/cache/SingleMediaHandler.kt'
  'internal/cache/SingleMediaHandlerFactory.kt'
  'internal/cache/CacheCleanupService.kt'
  'internal/script/JSEngineAndroidx.kt'
  'internal/script/JSEngineWebView.kt'
  'internal/script/ListenableFutureAwait.kt'
  'internal/text/TypefaceHolder.kt'
  'internal/utils/AdaptyUiVideoAccessor.kt'
  'internal/utils/bitmap.kt'
  'internal/utils/InsetWrapper.kt'
  'listeners/AdaptyFlowDefaultEventListener.kt'
  'internal/store/PurchaseEffectHandler.kt'
  'internal/store/DataLoadingEffectHandler.kt'
  'internal/store/AnalyticsEffectHandler.kt'
  'internal/ui/FlowDepsAdapters.kt'
  'preview/PreviewDependencies.kt'
  'preview/AdaptyFlowPreviewView.kt'
  '*.android.kt'
)

is_stay() {
  local rel=$1 pat
  for pat in "${stay_android[@]}"; do
    # shellcheck disable=SC2053
    [[ $rel == $pat ]] && return 0
  done
  return 1
}

import_re='^import (android\.|androidx\.(activity|core)\.|java\.|javax\.|com\.google\.gson|com\.adapty\.(models|errors|internal|utils|Adapty)|com\.adapty\.ui\.R$)'
# FQ uses outside import/package lines: android./java. (definition) plus androidx.activity/core, core SDK and Android R (stricter)
fq_re='(^|[^A-Za-z0-9_.])((android|java)\.[a-z]+\.|androidx\.(activity|core)\.|com\.adapty\.(models|errors|internal|utils)\.|com\.adapty\.Adapty\b|com\.adapty\.ui\.R\b)'

out=""
while IFS= read -r f; do
  rel=${f#"$root"/}
  is_stay "$rel" && continue
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
