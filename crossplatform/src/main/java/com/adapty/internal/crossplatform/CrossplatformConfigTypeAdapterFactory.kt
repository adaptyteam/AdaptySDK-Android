package com.adapty.internal.crossplatform

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class CrossplatformConfigTypeAdapterFactory(
    private val hasAdaptyUi: Boolean,
) : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!CrossplatformConfig::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val delegateConfigAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(CrossplatformConfig::class.java))

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<CrossplatformConfig>() {

            override fun write(out: JsonWriter, value: CrossplatformConfig) {
                delegateConfigAdapter.write(out, value)
            }

            override fun read(`in`: JsonReader): CrossplatformConfig? {
                val jsonObject = elementAdapter.read(`in`).asJsonObject
                val baseConfigJsonObject = JsonObject().also { jsonObject.add("base_config", it) }
                val apiKeyNode = jsonObject.removeNode("api_key").takeIf { it.second != null } ?: return null
                baseConfigJsonObject.addNodeIfNotEmpty(apiKeyNode)
                baseConfigJsonObject.addNodeIfNotEmpty("api_key_prefix" to JsonPrimitive(apiKeyNode.second.asString.split(".").getOrNull(0).orEmpty()))
                jsonObject.moveNodeIfExists(baseConfigJsonObject, "customer_user_id")
                jsonObject.moveNode(baseConfigJsonObject, "observer_mode", JsonPrimitive(false))
                jsonObject.moveNode(baseConfigJsonObject, "ip_address_collection_disabled", JsonPrimitive(false))

                jsonObject.removeNode("customer_identity_parameters").let { (_, value) ->
                    if (value is JsonObject) {
                        val obfuscatedAccountIdNode = value.removeNode("obfuscated_account_id")
                        baseConfigJsonObject.addNodeIfNotEmpty(obfuscatedAccountIdNode.copy(first = "gp_obfuscated_account_id"))
                    }
                }

                val adIdCollectionNode = jsonObject.removeNode("google_adid_collection_disabled")
                baseConfigJsonObject.addNode(adIdCollectionNode.copy(first = "ad_id_collection_disabled"), JsonPrimitive(false))

                val enablePendingPrepaidPlansNode = jsonObject.removeNode("google_enable_pending_prepaid_plans")
                baseConfigJsonObject.addNode(enablePendingPrepaidPlansNode.copy(first = "enable_pending_prepaid_plans"), JsonPrimitive(false))

                val localAccessLevelAllowedNode = jsonObject.removeNode("google_local_access_level_allowed")
                baseConfigJsonObject.addNode(localAccessLevelAllowedNode.copy(first = "allow_local_p_a_l"), JsonPrimitive(false))

                jsonObject.moveNode(baseConfigJsonObject, "server_cluster", JsonPrimitive("default"))

                if (hasAdaptyUi)
                    return gson.getDelegateAdapter(
                        this@CrossplatformConfigTypeAdapterFactory,
                        TypeToken.get(CrossplatformConfigWithUi::class.java),
                    ).fromJsonTree(jsonObject)

                jsonObject.remove("media_cache")

                return delegateConfigAdapter.fromJsonTree(jsonObject)
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}