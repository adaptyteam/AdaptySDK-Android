package com.adapty.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adapty.api.entity.paywalls.PaywallModel
import com.adapty.example.R

typealias ClickCallback = (PaywallModel) -> Unit

class PaywallAdapter(
    private val paywalls: List<PaywallModel>,
    private val onPaywallClick: ClickCallback
) :
    RecyclerView.Adapter<PaywallViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaywallViewHolder {
        return PaywallViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.paywall_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PaywallViewHolder, position: Int) {
        holder.bind(paywalls[position], onPaywallClick)
    }

    override fun getItemCount(): Int = paywalls.size
}

class PaywallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(paywall: PaywallModel, onPaywallClick: ClickCallback) {
        (itemView as TextView).apply {
            text = paywall.developerId
            setOnClickListener {
                onPaywallClick(paywall)
            }
        }
    }
}