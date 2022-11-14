package com.adapty.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            addFragment(MainFragment.newInstance(), false)
        }
    }

    fun addFragment(fragment: Fragment, addToBackStack: Boolean) {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, fragment)
            .apply { if (addToBackStack) this.addToBackStack(null) }
            .commit()
    }
}