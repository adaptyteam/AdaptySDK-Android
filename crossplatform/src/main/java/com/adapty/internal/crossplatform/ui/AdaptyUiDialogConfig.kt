package com.adapty.internal.crossplatform.ui

import android.os.Parcel
import android.os.Parcelable

class AdaptyUiDialogConfig(
    val title: String?,
    val content: String?,
    val defaultActionTitle: String?,
    val secondaryActionTitle: String?,
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(content)
        parcel.writeString(defaultActionTitle)
        parcel.writeString(secondaryActionTitle)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AdaptyUiDialogConfig> {
        override fun createFromParcel(parcel: Parcel): AdaptyUiDialogConfig {
            return AdaptyUiDialogConfig(parcel)
        }

        override fun newArray(size: Int): Array<AdaptyUiDialogConfig?> {
            return arrayOfNulls(size)
        }
    }
}