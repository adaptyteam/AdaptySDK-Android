package com.adapty.utils

import android.content.ContentResolver
import android.content.Context
import java.io.File

public sealed class FileLocation {
    internal class Uri(val uri: android.net.Uri): FileLocation()
    internal class Asset(val relativePath: String): FileLocation()

    public companion object {

        @JvmStatic
        public fun fromResId(context: Context, resId: Int): FileLocation =
            Uri(
                android.net.Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(context.packageName)
                    .appendPath("$resId")
                    .build()
            )

        @JvmStatic
        public fun fromAsset(relativePath: String): FileLocation =
            Asset(
                if (relativePath.startsWith(File.separatorChar))
                    relativePath.substring(1)
                else
                    relativePath
            )
    }
}