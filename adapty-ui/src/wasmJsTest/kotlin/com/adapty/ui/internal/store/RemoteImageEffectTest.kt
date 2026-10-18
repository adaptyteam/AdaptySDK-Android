@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.RemoteImage
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.ui.MediaSource
import com.adapty.ui.internal.ui.WebMediaSource
import com.adapty.ui.internal.utils.decodeBase64
import com.adapty.ui.internal.utils.decodeImage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class RemoteImageEffectTest {

    private val loaded = Image(Image.Source.Base64Str("AAAA"))

    private class RecordingMediaSource(private val result: Image?) : MediaSource {
        val requests = mutableListOf<RemoteImage>()
        var previewHandlerPassed = false

        override fun loadImage(
            remoteImage: RemoteImage,
            handlePreview: ((preview: Image) -> Unit)?,
            handleResult: ((image: Image) -> Unit)?,
        ) {
            requests += remoteImage
            previewHandlerPassed = handlePreview != null
            result?.let { handleResult?.invoke(it) }
        }
    }

    @Test
    fun loadRemoteImageDispatchesAssetLoaded() {
        val media = RecordingMediaSource(loaded)
        val messages = mutableListOf<Message>()
        val handler = RemoteImageEffectHandler(CoroutineScope(Dispatchers.Unconfined), media)
        val remote = RemoteImage("https://example.com/a.png", preview = null)

        handler.handle(Effect.LoadRemoteImage(remote, "bg")) { messages += it }

        assertEquals(listOf(remote), media.requests)
        assertTrue(!media.previewHandlerPassed, "like Android DataLoadingEffectHandler: no preview handler")
        val message = assertIs<Message.AssetLoaded>(messages.single())
        assertEquals("bg", message.id)
        assertSame(loaded, message.asset)
    }

    @Test
    fun otherEffectsAndMissingResultsDispatchNothing() {
        val media = RecordingMediaSource(null)
        val messages = mutableListOf<Message>()
        val handler = RemoteImageEffectHandler(CoroutineScope(Dispatchers.Unconfined), media)

        handler.handle(Effect.RefreshStateCache) { messages += it }
        assertTrue(media.requests.isEmpty())

        handler.handle(Effect.LoadRemoteImage(RemoteImage("https://example.com/a.png", null), "bg")) { messages += it }
        assertEquals(1, media.requests.size)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun webMediaSourceFetchesRemoteImage() = runTest {
        val image = loadThroughWebMediaSource(RemoteImage("${resourcesBase()}/star@3x.webp", preview = null))
        val source = assertIs<Image.Source.Base64Str>(image.source)
        val bitmap = assertNotNull(decodeImage(assertNotNull(decodeBase64(assertNotNull(source.imageBase64)))))
        assertEquals(63 to 60, bitmap.width to bitmap.height)
    }

    @Test
    fun webMediaSourceFallsBackToInlinePreview() = runTest {
        val preview = Image(Image.Source.Base64Str("AAAA"))
        var previewSeen: Image? = null
        val result = CompletableDeferred<Image>()
        WebMediaSource(MainScope()).loadImage(
            RemoteImage("${resourcesBase()}/missing.webp", preview),
            handlePreview = { previewSeen = it },
            handleResult = { result.complete(it) },
        )
        assertSame(preview, previewSeen)
        assertSame(preview, result.await(), "HTTP 404 falls back to the inline preview")
    }

    @Test
    fun webMediaSourceWithoutResultHandlerOnlyServesPreview() {
        var previewSeen: Image? = null
        WebMediaSource(MainScope()).loadImage(
            RemoteImage("https://example.invalid/a.png", preview = null),
            handlePreview = { previewSeen = it },
            handleResult = null,
        )
        assertNull(previewSeen)
    }

    private suspend fun loadThroughWebMediaSource(remote: RemoteImage): Image {
        val result = CompletableDeferred<Image>()
        WebMediaSource(MainScope()).loadImage(remote, handlePreview = null, handleResult = { result.complete(it) })
        return result.await()
    }

    private fun resourcesBase(): String =
        testResourcesBaseUrl().ifEmpty { fail("fixture base URL is not configured (karma.config.d)") }
}

@JsFun("() => (window.__karma__ && window.__karma__.config && window.__karma__.config.adaptyUiTestResourcesUrl) || ''")
private external fun testResourcesBaseUrl(): String
