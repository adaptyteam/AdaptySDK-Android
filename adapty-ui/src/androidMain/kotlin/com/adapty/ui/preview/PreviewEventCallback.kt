package com.adapty.ui.preview

/**
 * Receives the mocked purchase / restore lifecycle of a preview.
 */
public interface PreviewEventCallback {
    public fun onPurchaseStarted(productId: String) {}
    public fun onPurchaseFinished(productId: String, result: String) {}
    public fun onRestoreStarted() {}
    public fun onRestoreFinished(result: String) {}
}
