package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyFlowPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyWebPresentation
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class WebPurchaseArgsTypeAdapterFactory : TypeAdapterFactory {

    companion object {
        const val PAYWALL = "paywall"
        const val PRODUCT = "product"
        const val OPEN_IN = "open_in"
        const val BROWSER_OUT_APP = "browser_out_app"
        const val BROWSER_IN_APP = "browser_in_app"
    }

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!WebPaywallArgs::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val paywallAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(AdaptyFlowPaywall::class.java))

        val productAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(AdaptyPaywallProduct::class.java))

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<WebPaywallArgs>() {

            override fun write(out: JsonWriter, value: WebPaywallArgs) {}

            override fun read(`in`: JsonReader): WebPaywallArgs? {
                val jsonObject = elementAdapter.read(`in`).asJsonObject
                val presentation = kotlin.runCatching {
                    when (jsonObject.get(OPEN_IN)?.asString) {
                        BROWSER_IN_APP -> AdaptyWebPresentation.InAppBrowser
                        BROWSER_OUT_APP -> AdaptyWebPresentation.ExternalBrowser
                        else -> null
                    }
                }.getOrNull()
                val paywallJson = kotlin.runCatching { jsonObject.getAsJsonObject(PAYWALL) }.getOrNull()
                if (paywallJson != null)
                    return WebPaywallArgs.Paywall(
                        paywallAdapter.fromJsonTree(paywallJson),
                        presentation,
                    )
                val productJson = kotlin.runCatching { jsonObject.getAsJsonObject(PRODUCT) }.getOrNull()
                if (productJson != null)
                    return WebPaywallArgs.Product(
                        productAdapter.fromJsonTree(productJson),
                        presentation,
                    )
                return null
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}