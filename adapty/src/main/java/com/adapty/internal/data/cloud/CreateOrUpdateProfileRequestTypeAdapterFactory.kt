package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.requests.CreateOrUpdateProfileRequest
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CreateOrUpdateProfileRequestTypeAdapterFactory : TypeAdapterFactory {

    private companion object {
        const val DATA = "data"
        const val ATTRS = "attributes"
        const val META = "installation_meta"
        const val STORE_COUNTRY = "store_country"
    }

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!CreateOrUpdateProfileRequest::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val delegateAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(CreateOrUpdateProfileRequest::class.java))
        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<CreateOrUpdateProfileRequest>() {

            override fun write(out: JsonWriter, value: CreateOrUpdateProfileRequest) {
                val jsonObject = delegateAdapter.toJsonTree(value).asJsonObject
                val attrs = jsonObject.getAsJsonObjectOrNull(DATA)?.getAsJsonObjectOrNull(ATTRS)
                if (attrs != null) {
                    val storeCountry = attrs.getAsJsonObjectOrNull(META)?.remove(STORE_COUNTRY)
                    if (storeCountry != null) attrs.add(STORE_COUNTRY, storeCountry)
                }
                elementAdapter.write(out, jsonObject)
            }

            override fun read(`in`: JsonReader): CreateOrUpdateProfileRequest? {
                return null
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }

    private fun JsonObject.getAsJsonObjectOrNull(memberName: String) =
        get(memberName)?.takeIf(JsonElement::isJsonObject)?.asJsonObject
}