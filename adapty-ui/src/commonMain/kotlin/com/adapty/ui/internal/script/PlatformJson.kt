package com.adapty.ui.internal.script

// Gson-shaped JSON (objects → Map, arrays → List, numbers → Double) for the JS state layer.
internal expect object PlatformJson {
    fun parse(json: String): Any?

    fun stringify(value: Any?): String
}
