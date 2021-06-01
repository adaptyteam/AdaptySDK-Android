package com.adapty.example

import android.widget.Toast
import androidx.fragment.app.Fragment

fun Fragment.showToast(text: CharSequence) {
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}