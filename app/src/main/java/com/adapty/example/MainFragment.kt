package com.adapty.example

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adapty.Adapty
import com.adapty.listeners.OnProfileUpdatedListener
import com.adapty.models.AdaptyAttributionSource
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyProfileParameters
import com.adapty.utils.AdaptyResult
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
            Adapty.restorePurchases { result ->
                last_response_result.text = when (result) {
                    is AdaptyResult.Success -> "Profile:\n${result.value}"
                    is AdaptyResult.Error -> "error:\n${result.error.message}"
                }
            }
        }

        get_profile.setOnClickListener {
            Adapty.getProfile { result ->
                last_response_result.text =
                    when (result) {
                        is AdaptyResult.Success -> {
                            "profile: ${result.value}"
                        }
                        is AdaptyResult.Error -> {
                            "error:\n${result.error.message}"
                        }
                    }
            }
        }

        get_paywall_by_id.setOnClickListener {
            progressDialog.show()

            Adapty.getPaywall(paywall_id.text.toString()) { result ->
                when (result) {
                    is AdaptyResult.Success -> {
                        val paywall = result.value
                        Adapty.getPaywallProducts(paywall) { productResult ->
                            progressDialog.cancel()

                            when (productResult) {
                                is AdaptyResult.Success -> {
                                    last_response_result.text =
                                        "Paywall: $paywall\n\nProducts: ${productResult.value}"

                                    Adapty.logShowPaywall(paywall)

                                    (activity as? MainActivity)?.addFragment(
                                        ProductListFragment.newInstance(productResult.value),
                                        true
                                    )
                                }
                                is AdaptyResult.Error -> {
                                    /**
                                     * If the error code is `AdaptyErrorCode.NO_PRODUCT_IDS_FOUND`, please make sure you have changed your applicationId.
                                     *
                                     * In order to receive products and make purchases,
                                     * please change sample's applicationId in app/build.gradle to yours
                                     */
                                    last_response_result.text =
                                        "error:\n${productResult.error.message}"
                                }
                            }
                        }
                    }
                    is AdaptyResult.Error -> {
                        last_response_result.text = "error:\n${result.error.message}"
                        progressDialog.cancel()
                    }
                }
            }
        }

        update_profile.setOnClickListener {
            val params = AdaptyProfileParameters.Builder()
                .withEmail("email@example.com")
                .withBirthday(AdaptyProfile.Date(1970, 1, 3))
                .withCustomAttribute("key1", "test")
                .withCustomAttribute("key3", 5.0)
                .withRemovedCustomAttribute("key2")
                .build()

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
            Adapty.updateAttribution(attribution, AdaptyAttributionSource.CUSTOM) { error ->
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

        Adapty.setOnProfileUpdatedListener(object : OnProfileUpdatedListener {
            override fun onProfileReceived(profile: AdaptyProfile) {
                showToast("Profile:\n${profile}")
            }
        })
    }
}