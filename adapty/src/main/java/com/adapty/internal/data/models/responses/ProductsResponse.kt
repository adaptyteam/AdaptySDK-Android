package com.adapty.internal.data.models.responses

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.ProductDto
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductsResponse(
    @SerializedName("data")
    val data: ArrayList<ProductDto>?,
)