@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui

import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.RemoteImage
import com.adapty.ui.internal.platform.ERROR
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.log
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.decodeBase64
import com.adapty.ui.internal.utils.decodeImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.js.Promise

/**
 * Browser media source: no disk cache. The inline (base64) preview goes to [loadImage]'s `handlePreview`;
 * the remote image is fetched with `fetch` and delivered once through `handleResult` as a base64 image.
 * A failed fetch (network, CORS, timeout, undecodable bytes) is logged and falls back to the inline preview.
 */
internal class WebMediaSource(
    private val scope: CoroutineScope = MainScope(),
    private val timeoutMs: Int = FETCH_TIMEOUT_MS,
) : MediaSource {

    override fun loadImage(
        remoteImage: RemoteImage,
        handlePreview: ((preview: Image) -> Unit)?,
        handleResult: ((image: Image) -> Unit)?,
    ) {
        val preview = remoteImage.preview?.takeIf { it.source is Image.Source.Base64Str }
        if (preview != null) handlePreview?.invoke(preview)
        if (handleResult == null) return
        scope.launch {
            val fetched = try {
                val base64 = fetchAsBase64(remoteImage.url, timeoutMs).await<JsString>().toString()
                val bytes = decodeBase64(base64)
                if (bytes == null || decodeImage(bytes) == null) {
                    log(ERROR) { "$LOG_PREFIX_ERROR web media: undecodable image ${remoteImage.url}" }
                    null
                } else {
                    Image(Image.Source.Base64Str(base64))
                }
            } catch (e: Throwable) {
                log(ERROR) { "$LOG_PREFIX_ERROR web media: fetch failed ${remoteImage.url}: ${e.message}" }
                null
            }
            (fetched ?: preview)?.let(handleResult)
        }
    }

    private companion object {
        const val FETCH_TIMEOUT_MS = 15_000
    }
}

@JsFun(
    """(url, timeoutMs) => {
        const ctl = new AbortController();
        const timer = setTimeout(() => ctl.abort(), timeoutMs);
        return fetch(url, { signal: ctl.signal })
            .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.arrayBuffer(); })
            .then(buf => {
                const bytes = new Uint8Array(buf);
                let s = '';
                for (let i = 0; i < bytes.length; i += 0x8000) s += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
                return btoa(s);
            })
            .finally(() => clearTimeout(timer));
    }"""
)
private external fun fetchAsBase64(url: String, timeoutMs: Int): Promise<JsString>
