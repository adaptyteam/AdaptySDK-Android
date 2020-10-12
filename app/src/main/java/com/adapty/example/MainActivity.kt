package com.adapty.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.adapty.Adapty

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            addFragment(MainFragment.newInstance(), false)
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Adapty.handlePromoIntent(intent) { promo, error ->
                //your logic for callback
            }
        ) {
            //your logic for the case user did click on promo notification,
            //for example show loading indicator
        } else {
            //your logic for other cases
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