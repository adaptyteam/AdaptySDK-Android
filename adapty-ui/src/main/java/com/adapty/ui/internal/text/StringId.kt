@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.utils.DEFAULT_PRODUCT_GROUP

@InternalAdaptyApi
public sealed class StringId {
    public class Str internal constructor(internal val value: String): StringId()
    public class Product internal constructor(
        internal val productId: String?,
        internal val productGroupId: String,
        internal val suffix: String?,
    ): StringId()
}

internal fun Any.toStringId(): StringId? {
    return (this as? String)?.takeIf { it.isNotEmpty() }
        ?.let { value -> StringId.Str(value) }
        ?: run {
            (this as? Map<*, *>)?.let { stringIdObject ->
                val type = stringIdObject["type"]
                if (type != "product")
                    throw adaptyError(
                        message = "Unsupported type in string_id in Text ($type)",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                val productId = stringIdObject["id"] as? String
                val productGroup = (stringIdObject["group_id"] as? String)?.takeIf { it.isNotEmpty() }
                    ?: DEFAULT_PRODUCT_GROUP
                val suffix = stringIdObject["suffix"] as? String
                StringId.Product(productId, productGroup, suffix)
            }
        }
}