package com.adapty.ui.internal.cache

import androidx.annotation.RestrictTo
import com.adapty.ui.AdaptyUI
import kotlin.concurrent.Volatile

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MediaCacheConfigManager {

    @Volatile
    var currentCacheConfig = AdaptyUI.MediaCacheConfiguration.Builder().build()
}