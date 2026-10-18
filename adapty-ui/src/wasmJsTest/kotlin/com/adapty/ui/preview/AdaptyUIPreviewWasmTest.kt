@file:OptIn(com.adapty.ui.internal.platform.InternalAdaptyUiApi::class)

package com.adapty.ui.preview

import com.adapty.ui.internal.script.PlatformJson
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class AdaptyUIPreviewWasmTest {

    @Test
    fun createFromBundle_calmOnboarding() = runTest {
        val main = fetchJsonObject("main.json")
        val config = AdaptyUIPreview.createFromBundle(
            format = main["format"] as String,
            elements = emptyMap(),
            screens = calmScreenIds.associateWith { fetchJsonObject("$it.screen.json") },
            navigators = mapOf("default" to fetchJsonObject("default.navigator.json")),
            assets = (main["assets"] as List<*>).map { it.asJsonObject() },
            localizations = listOf((main["localizations"]).asJsonObject()),
            script = fetchFixture("script.js"),
            initialScreenId = main["initial_screen_id"] as String,
            initialNavigatorId = main["initial_navigator_id"] as String,
        )

        assertCalmConfig(config)
    }

    @Test
    fun platformJson_parsesGsonShapes() {
        val parsed = PlatformJson.parse("""{"a":0,"b":[1.5,"x",true,null],"c":{"d":-2}}""").asJsonObject()
        assertTrue(parsed["a"] is Double, "numbers are Double like Gson")
        assertTrue(parsed["b"] == listOf(1.5, "x", true, null))
        assertTrue(parsed["c"].asJsonObject()["d"] == -2.0)
        val roundTrip = PlatformJson.parse(PlatformJson.stringify(parsed))
        assertTrue(roundTrip == parsed, "stringify round-trips: $roundTrip")
    }
}

@Suppress("UNCHECKED_CAST")
private fun Any?.asJsonObject(): Map<String, Any?> =
    this as? Map<String, Any?> ?: fail("expected a JSON object, got ${this?.let { it::class.simpleName }}")

private suspend fun fetchJsonObject(name: String): Map<String, Any?> = PlatformJson.parse(fetchFixture(name)).asJsonObject()

private suspend fun fetchFixture(name: String): String {
    val base = fixturesBaseUrl()
    if (base.isEmpty()) {
        fail("fixture base URL is not configured: karma.config.d/fixtures.js or the generated paths.js was not loaded")
    }
    val url = "$base/$name"
    val response = fetchText(url).await<JsAny>()
    val status = statusOf(response)
    if (status == 404) {
        fail("fixture fetch returned 404 for $url: Karma does not serve the calm-onboarding dir (check karma.config.d)")
    }
    if (status != 200) fail("fixture fetch returned HTTP $status for $url")
    return bodyOf(response)
}

@JsFun("() => (window.__karma__ && window.__karma__.config && window.__karma__.config.adaptyUiFixturesUrl) || ''")
private external fun fixturesBaseUrl(): String

@JsFun("(url) => fetch(url).then(r => r.text().then(t => ({ status: r.status, body: t })))")
private external fun fetchText(url: String): Promise<JsAny>

@JsFun("(r) => r.status")
private external fun statusOf(response: JsAny): Int

@JsFun("(r) => r.body")
private external fun bodyOf(response: JsAny): String
