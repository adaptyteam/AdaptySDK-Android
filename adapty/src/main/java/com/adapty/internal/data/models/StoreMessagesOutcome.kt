package com.adapty.internal.data.models

internal sealed class StoreMessagesOutcome {

    object HostUnavailable : StoreMessagesOutcome()

    class Rejected(val billingResponseCode: Int) : StoreMessagesOutcome()

    class Completed(val responseCode: Int) : StoreMessagesOutcome()
}
