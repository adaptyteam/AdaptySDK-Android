package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyInstallationDetails
import com.adapty.models.AdaptyInstallationStatus
import com.adapty.models.AdaptyInstallationStatus.*
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyInstallationStatusTypeAdapterFactory : TypeAdapterFactory {

    companion object {
        const val STATUS = "status"
        const val DETAILS = "details"
    }

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!AdaptyInstallationStatus::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val detailsAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(AdaptyInstallationDetails::class.java))

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<AdaptyInstallationStatus>() {

            override fun write(out: JsonWriter, value: AdaptyInstallationStatus) {
                val jsonObject = JsonObject().apply {
                    when (value) {
                        is Determined.Success -> {
                            add(DETAILS, detailsAdapter.toJsonTree(value.details).asJsonObject)
                            addProperty(STATUS, "determined")
                        }

                        is Determined.NotAvailable -> {
                            addProperty(STATUS, "not_available")
                        }

                        is NotDetermined -> {
                            addProperty(STATUS, "not_determined")
                        }
                    }
                }
                elementAdapter.write(out, jsonObject)
            }

            override fun read(`in`: JsonReader): AdaptyInstallationStatus? {
                return null
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}