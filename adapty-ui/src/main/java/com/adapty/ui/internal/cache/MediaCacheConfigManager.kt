package com.adapty.ui.internal.cache

import androidx.annotation.RestrictTo
import com.adapty.ui.AdaptyUI

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MediaCacheConfigManager {

    @Volatile
    var currentCacheConfig = AdaptyUI.MediaCacheConfiguration.Builder().build()
}