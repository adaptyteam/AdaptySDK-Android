package com.adapty.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adapty.example.R
import com.adapty.models.PaywallModel
import kotlinx.android.synthetic.main.paywall_item.view.*

typealias ClickCallback = (PaywallModel) -> Unit

class PaywallAdapter(
    private val paywalls: List<PaywallModel>,
    private val onPaywallClick: ClickCallback,
    private val onVisualPaywallClick: ClickCallback
) :
    RecyclerView.Adapter<PaywallViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaywallViewHolder {
        return PaywallViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.paywall_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PaywallViewHolder, position: Int) {
        holder.bind(paywalls[position], onPaywallClick, onVisualPaywallClick)
    }

    override fun getItemCount(): Int = paywalls.size
}

class PaywallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(paywall: PaywallModel, onPaywallClick: ClickCallback, onVisualPaywallClick: ClickCallback) {
        itemView.setOnClickListener {
            onPaywallClick(paywall)
        }
        with(itemView) {
            title.text = paywall.developerId
            if (paywall.visualPaywall.isNullOrEmpty()) {
                show_visual_paywall.visibility = View.GONE
            } else {
                show_visual_paywall.visibility = View.VISIBLE
                show_visual_paywall.setOnClickListener {
                    onVisualPaywallClick(paywall)
                }
            }
        }
    }
}