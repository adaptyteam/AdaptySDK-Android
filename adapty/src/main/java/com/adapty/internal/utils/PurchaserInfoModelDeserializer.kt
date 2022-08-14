package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.models.AccessLevelInfoModel
import com.adapty.models.NonSubscriptionInfoModel
import com.adapty.models.PurchaserInfoModel
import com.adapty.models.SubscriptionInfoModel
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PurchaserInfoModelDeserializer(
    private val gson: Gson,
) : JsonDeserializer<PurchaserInfoModel> {

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): PurchaserInfoModel {
        val jsonObject = json.asJsonObject
        return PurchaserInfoModel(
            jsonObject.get("profileId")?.asString.orEmpty(),
            jsonObject.get("customerUserId")?.asString,
            jsonObject.get("accessLevels")?.asJsonObject?.let { accessLevels ->
                gson.fromJson<Map<String, AccessLevelInfoModel>>(
                    accessLevels,
                    object : TypeToken<Map<String, AccessLevelInfoModel>>() {}.type
                )
            }.orEmpty(),
            jsonObject.get("subscriptions")?.asJsonObject?.let { subs ->
                gson.fromJson<Map<String, SubscriptionInfoModel>>(
                    subs,
                    object : TypeToken<Map<String, SubscriptionInfoModel>>() {}.type
                )
            }.orEmpty(),
            jsonObject.get("nonSubscriptions")?.asJsonObject?.let { nonSubs ->
                gson.fromJson<Map<String, List<NonSubscriptionInfoModel>>>(
                    nonSubs,
                    object : TypeToken<Map<String, List<NonSubscriptionInfoModel>>>() {}.type
                )
            }.orEmpty(),
            jsonObject.get("customAttributes")?.asJsonObject?.let { customAttributes ->
                gson.fromJson<Map<String, Any>>(
                    customAttributes,
                    object : TypeToken<Map<String, Any>>() {}.type
                )
            }.orEmpty(),
        )
    }
}