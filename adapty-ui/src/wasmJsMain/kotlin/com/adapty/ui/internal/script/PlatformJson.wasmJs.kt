package com.adapty.ui.internal.script

import com.adapty.ui.internal.platform.webStub

internal actual object PlatformJson {
    actual fun parse(json: String): Any? {
        webStub("PlatformJson.parse")
        return null
    }

    actual fun stringify(value: Any?): String {
        webStub("PlatformJson.stringify")
        return "null"
    }
}
