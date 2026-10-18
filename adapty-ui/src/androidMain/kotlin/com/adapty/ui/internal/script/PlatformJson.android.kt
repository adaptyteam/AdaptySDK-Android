package com.adapty.ui.internal.script

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Gson-shaped JSON (objects → Map, arrays → List, numbers → Double) for the JS state layer.
internal object PlatformJson {

    // AdaptyUI swaps in the core "base" Gson once Adapty is ready (as before); preview keeps a plain Gson()
    @Volatile
    internal var gson: Gson = Gson()

    fun parse(json: String): Any? = gson.fromJson(json, Any::class.java)

    fun stringify(value: Any?): String = gson.toJson(value)

    inline fun <reified T> parseAs(json: String): T? = gson.fromJson<T>(json, object : TypeToken<T>() {}.type)
}
