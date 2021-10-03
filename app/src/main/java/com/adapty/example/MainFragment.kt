package com.adapty.example

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adapty.Adapty
import com.adapty.listeners.OnPromoReceivedListener
import com.adapty.listeners.OnPurchaserInfoUpdatedListener
import com.adapty.models.AttributionType
import com.adapty.models.Date
import com.adapty.models.PromoModel
import com.adapty.models.PurchaserInfoModel
import com.adapty.utils.ProfileParameterBuilder
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
                    error?.let { "error:\n${error.message}" } ?: "Purchaser Info:\n$purchaserInfo\n\nValidationResults:\n$googleValidationResultList"
            }
        }

        get_purchaser_info.setOnClickListener {
            Adapty.getPurchaserInfo { purchaserInfo, error ->
                last_response_result.text =
                    error?.let { "error:\n${error.message}" }
                        ?: "purchaser info: $purchaserInfo"
            }
        }

        get_purchaser_info_force_update.setOnClickListener {
            Adapty.getPurchaserInfo(forceUpdate = true) { purchaserInfo, error ->
                last_response_result.text =
                    error?.let { "error:\n${error.message}" }
                        ?: "purchaser info: $purchaserInfo"
            }
        }

        get_paywalls.setOnClickListener {
            progressDialog.show()

            Adapty.getPaywalls { paywalls, products, error ->
                last_response_result.text =
                    error?.let { "error:\n${error.message}" }
                        ?: "Paywalls are fetched successfully"

                progressDialog.hide()
                paywalls?.let {
                    (activity as? MainActivity)?.addFragment(
                        PaywallListFragment.newInstance(paywalls),
                        true
                    )
                }
            }
        }

        get_paywalls_force_update.setOnClickListener {
            progressDialog.show()

            Adapty.getPaywalls(forceUpdate = true) { paywalls, products, error ->
                last_response_result.text =
                    error?.let { "error:\n${error.message}" }
                        ?: "Paywalls are fetched successfully"

                progressDialog.hide()
                paywalls?.let {
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
                    error?.let { "error:\n${error.message}" }
                        ?: "promo:\n$promo"
            }
        }

        update_profile.setOnClickListener {
            val params = ProfileParameterBuilder()
                .withEmail("email@example.com")
                .withBirthday(Date(1970, 1, 3))
                .withCustomAttributes(mapOf("key1" to "test", "key2" to 5))

            Adapty.updateProfile(params) { error ->
                last_response_result.text = error?.let { "error:\n${error.message}" } ?: "Profile updated"
            }
        }

        update_custom_attribution.setOnClickListener {
            //you can only use the keys below, but all of them are optional
            val attribution = mapOf(
                "status" to "non_organic", //the only possible values for this key: non_organic|organic|unknown
                "channel" to "Google Ads",
                "campaign" to "Adapty in-app",
                "ad_group" to "adapty ad_groupadapty ad_groupadapty ad",
                "ad_set" to "adapty ad_set",
                "creative" to "12312312312312"
            )
            Adapty.updateAttribution(attribution, AttributionType.CUSTOM) { error ->
                last_response_result.text = error?.message ?: "success"
            }
        }

        identify.setOnClickListener {
            Adapty.identify(customer_user_id.text.toString()) { error ->
                last_response_result.text = error?.let { "error:\n${error.message}" } ?: "User identified"
            }
        }

        logout.setOnClickListener {
            Adapty.logout { error ->
                last_response_result.text = error?.let { "error:\n${error.message}" } ?: "User logged out"
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
}