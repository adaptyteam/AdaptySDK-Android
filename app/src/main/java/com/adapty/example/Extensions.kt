package com.adapty.example

import android.view.View
import android.widget.Toast
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

fun Fragment.showToast(text: CharSequence) {
    val context = context ?: return
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}

fun View.onReceiveSystemBarsInsets(action: (insets: Insets) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        ViewCompat.setOnApplyWindowInsetsListener(this, null)
        action(systemBarInsets)
        insets
    }
}