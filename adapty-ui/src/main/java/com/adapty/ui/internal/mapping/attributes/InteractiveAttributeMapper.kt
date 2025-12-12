@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.attributes

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyWebPresentation
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.ui.element.Condition
import com.adapty.ui.internal.utils.DEFAULT_PRODUCT_GROUP

internal class InteractiveAttributeMapper {
    fun mapAction(item: Map<*, *>): Action {
        return when(item["type"]) {
            "open_url" -> {
                val url = (item["url"] as? String)
                    ?: throw adaptyError(
                        message = "Couldn't find 'url' for an 'open_url' action",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Action.OpenUrl(url)
            }
            "custom" -> {
                val customId = (item["custom_id"] as? String)
                    ?: throw adaptyError(
                        message = "Couldn't find 'custom_id' for a 'custom' action",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Action.Custom(customId)
            }
            "select_product" -> {
                val productId = (item["product_id"] as? String)
                    ?: throw adaptyError(
                        message = "Couldn't find 'product_id' for a 'select_product' action",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Action.SelectProduct(productId, mapGroupId(item))
            }
            "unselect_product" -> Action.UnselectProduct(mapGroupId(item))
            "purchase_product" -> {
                val productId = (item["product_id"] as? String)
                    ?: throw adaptyError(
                        message = "Couldn't find 'product_id' for a 'purchase_product' action",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Action.PurchaseProduct(productId)
            }
            "web_purchase_product" -> {
                val productId = (item["product_id"] as? String)
                    ?: throw adaptyError(
                        message = "Couldn't find 'product_id' for a 'web_purchase_product' action",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                val presentation = mapWebPurchasePresentation(item["open_in"])
                Action.WebPurchaseProduct(productId, presentation)
            }
            "web_purchase_paywall" -> {
                val presentation = mapWebPurchasePresentation(item["open_in"])
                Action.WebPurchasePaywall(presentation)
            }
            "purchase_selected_product" -> Action.PurchaseSelectedProduct(mapGroupId(item))
            "web_purchase_selected_product" -> {
                val presentation = mapWebPurchasePresentation(item["open_in"])
                Action.WebPurchaseSelectedProduct(mapGroupId(item), presentation)
            }
            "restore" -> Action.RestorePurchases
            "open_screen" -> {
                val screenId = (item["screen_id"] as? String)
                    ?: throw adaptyError(
                        message = "Couldn't find 'screen_id' for a 'open_screen' action",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Action.OpenScreen(screenId)
            }
            "close_screen" -> Action.CloseCurrentScreen
            "switch" -> {
                val sectionId = (item["section_id"] as? String)
                    ?: throw adaptyError(
                        message = "Couldn't find 'section_id' for a 'switch' action",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                val index = (item["index"] as? Number)?.toInt()
                    ?: throw adaptyError(
                        message = "Couldn't find 'index' for a 'switch' action",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Action.SwitchSection(sectionId, index)
            }
            "close" -> Action.ClosePaywall
            else -> Action.Unknown
        }
    }

    fun mapCondition(item: Map<*, *>): Condition {
        return when(item["type"]) {
            "selected_section" -> {
                val sectionId = (item["section_id"] as? String)
                    ?: throw adaptyError(
                        message = "Couldn't find 'section_id' for a 'selected_section' condition",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                val index = (item["index"] as? Number)?.toInt()
                    ?: throw adaptyError(
                        message = "Couldn't find 'index' for a 'selected_section' condition",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Condition.SelectedSection(sectionId, index)
            }
            "selected_product" -> {
                val productId = (item["product_id"] as? String)
                    ?: throw adaptyError(
                        message = "Couldn't find 'product_id' for a 'selected_product' condition",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Condition.SelectedProduct(productId, mapGroupId(item))
            }
            else -> Condition.Unknown
        }
    }

    private fun mapGroupId(item: Map<*, *>) =
        (item["group_id"] as? String) ?: DEFAULT_PRODUCT_GROUP

    private fun mapWebPurchasePresentation(item: Any?) =
        when(item) {
            "browser_in_app" -> AdaptyWebPresentation.Companion.InAppBrowser
            else -> AdaptyWebPresentation.Companion.ExternalBrowser
        }
}