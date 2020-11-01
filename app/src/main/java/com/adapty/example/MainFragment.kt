package com.adapty.example

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.adapty.Adapty
import com.adapty.api.AttributionType
import com.adapty.api.entity.DataState
import com.adapty.api.entity.paywalls.OnPromoReceivedListener
import com.adapty.api.entity.paywalls.PromoModel
import com.adapty.api.entity.profile.update.Date
import com.adapty.api.entity.profile.update.ProfileParameterBuilder
import com.adapty.api.entity.purchaserInfo.OnPurchaserInfoUpdatedListener
import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel
import kotlinx.android.synthetic.main.fragment_main.*

/**
 * In order to receive full info about the products and make purchases,
 * please change sample's applicationId in app/build.gradle to yours
 */

class MainFragment : Fragment(R.layout.fragment_main) {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restore_purchases.setOnClickListener {
            Adapty.restorePurchases { purchaserInfo, googleValidationResultList, error ->
                last_response_result.text =
                    error?.let { "error:\n$error" } ?: "Purchaser Info:\n$purchaserInfo\n\nValidationResults:\n$googleValidationResultList"
            }
        }

        get_purchaser_info.setOnClickListener {
            Adapty.getPurchaserInfo { purchaserInfo, state, error ->
                last_response_result.text =
                    error?.let { "error:\n$error" }
                        ?: "state: $state\npurchaser info: $purchaserInfo"
            }
        }

        get_paywalls.setOnClickListener {
            progressDialog.show()

            Adapty.getPaywalls { paywalls, products, state, error ->
                last_response_result.text =
                    error?.let { "error:\n$error" }
                        ?: "state: $state\npaywalls: $paywalls\n\nproducts: $products"

                if (state == DataState.SYNCED) {
                    progressDialog.hide()
                    (activity as? MainActivity)?.addFragment(
                        PaywallListFragment.newInstance(paywalls),
                        true
                    )
                }
            }
        }

        get_promo.setOnClickListener {
            Adapty.getPromo { promo, error ->
                last_response_result.text =
                    error?.let { "error:\n$error" }
                        ?: "promo:\n$promo"
            }
        }

        update_profile.setOnClickListener {
            val params = ProfileParameterBuilder()
                .withEmail("email@example.com")
                .withBirthday(Date(1970, 1, 3))
                .withCustomAttributes(mapOf("key1" to "test", "key2" to 5))

            Adapty.updateProfile(params) { error ->
                last_response_result.text = error?.let { "error:\n$error" } ?: "Profile updated"
            }
        }

        update_custom_attribution.setOnClickListener {
            Adapty.updateAttribution(hashMapOf("key1" to "test1", "key2" to true), AttributionType.CUSTOM) { error ->
                last_response_result.text = error ?: "success"
            }
        }

        identify.setOnClickListener {
            Adapty.identify(customer_user_id.text.toString()) { error ->
                last_response_result.text = error?.let { "error:\n$error" } ?: "User identified"
            }
        }

        logout.setOnClickListener {
            Adapty.logout { error ->
                last_response_result.text = error?.let { "error:\n$error" } ?: "User logged out"
            }
        }

        Adapty.setOnPurchaserInfoUpdatedListener(object : OnPurchaserInfoUpdatedListener {
            override fun onPurchaserInfoReceived(purchaserInfo: PurchaserInfoModel) {
                showToast("Updated purchase info:\n${purchaserInfo}")
            }
        })

        Adapty.setOnPromoReceivedListener(object : OnPromoReceivedListener {
            override fun onPromoReceived(promo: PromoModel) {
                showToast("New promo received:\n${promo}")
            }
        })
    }

    private fun showToast(text: CharSequence) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}