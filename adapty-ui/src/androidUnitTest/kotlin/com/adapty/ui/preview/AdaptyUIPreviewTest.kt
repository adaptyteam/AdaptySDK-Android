@file:OptIn(com.adapty.internal.utils.InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.preview

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdaptyUIPreviewTest {

    @Test
    fun createFromBundle_calmOnboarding() {
        val main = JSONObject(readFixture("main.json"))
        val config = AdaptyUIPreview.createFromBundle(
            format = main.getString("format"),
            elements = emptyMap(),
            screens = calmScreenIds.associateWith { JSONObject(readFixture("$it.screen.json")).toMap() },
            navigators = mapOf("default" to JSONObject(readFixture("default.navigator.json")).toMap()),
            assets = main.getJSONArray("assets").toListOfMaps(),
            localizations = listOf(main.getJSONObject("localizations").toMap()),
            script = readFixture("script.js"),
            initialScreenId = main.getString("initial_screen_id"),
            initialNavigatorId = main.getString("initial_navigator_id"),
        )

        assertCalmConfig(config)
    }

    @Test
    fun createFromBundle_localizationsSingleObjectWrapped() {
        val main = JSONObject(readFixture("main.json"))
        // The calm bundle stores one localization object, not an array; callers wrap it
        val rawLocalization = main.get("localizations")
        assertTrue(rawLocalization is JSONObject)
        val localization = rawLocalization.toMap()

        val config = AdaptyUIPreview.createFromBundle(
            format = AdaptyUIPreview.Format_5_0_0,
            screens = mapOf("screen_welcome" to JSONObject(readFixture("screen_welcome.screen.json")).toMap()),
            assets = main.getJSONArray("assets").toListOfMaps(),
            localizations = listOf(localization),
        )

        assertEquals("en", config.localizationId)
        @Suppress("UNCHECKED_CAST")
        val stringIds = (localization["strings"] as List<Map<String, Any?>>).mapNotNull { it["id"] as? String }
        assertTrue(stringIds.isNotEmpty())
        assertTrue(config.texts.keys.containsAll(stringIds), "every localized string is mapped")
        // No navigators given: a default screen_holder navigator is synthesized
        assertEquals(setOf("default"), config.navigators.keys)
        assertTrue("instanceId: \"screen_welcome\"" in config.initialScript)
    }

    @Test
    fun previewViewModelArgs_buildWithoutActivatedSdk() {
        val context = RuntimeEnvironment.getApplication()
        val first = createPreviewViewModelArgs(context, previewEventCallback = null, onMissingTag = null)
        val second = createPreviewViewModelArgs(context, previewEventCallback = null, onMissingTag = null)

        assertEquals(context.packageName, first.metaInfoRetriever.applicationId)
        assertSame(first.cacheRepository, second.cacheRepository)
        assertSame(first.metaInfoRetriever, second.metaInfoRetriever)
        assertSame(first.stateHandler, second.stateHandler, "one JS engine shared by all previews")
        assertTrue(first.flowKey != second.flowKey)
    }

    private fun readFixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("calm-onboarding/$name")) {
            "missing fixture $name"
        }.bufferedReader().use { it.readText() }
}

private fun JSONObject.toMap(): Map<String, Any?> =
    keys().asSequence().associateWith { key -> unwrap(get(key)) }

private fun JSONArray.toListOfMaps(): List<Map<String, Any?>> =
    (0 until length()).map { getJSONObject(it).toMap() }

private fun unwrap(value: Any?): Any? = when (value) {
    is JSONObject -> value.toMap()
    is JSONArray -> (0 until value.length()).map { unwrap(value.get(it)) }
    JSONObject.NULL -> null
    else -> value
}
