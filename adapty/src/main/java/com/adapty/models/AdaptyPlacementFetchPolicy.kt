package com.adapty.models

public sealed class AdaptyPlacementFetchPolicy {

    internal class ReloadRevalidatingCacheData private constructor(): AdaptyPlacementFetchPolicy() {

        internal companion object {
            fun create() = ReloadRevalidatingCacheData()
        }

        override fun toString(): String {
            return "ReloadRevalidatingCacheData"
        }
    }

    internal class ReturnCacheDataElseLoad private constructor(): AdaptyPlacementFetchPolicy() {

        internal companion object {
            fun create() = ReturnCacheDataElseLoad()
        }

        override fun toString(): String {
            return "ReturnCacheDataElseLoad"
        }
    }

    internal class ReturnCacheDataIfNotExpiredElseLoad private constructor(public val maxAgeMillis: Long) : AdaptyPlacementFetchPolicy() {

        internal companion object {
            fun create(maxAgeMillis: Long) = ReturnCacheDataIfNotExpiredElseLoad(maxAgeMillis)
        }

        override fun toString(): String {
            return "ReturnCacheDataIfNotExpiredElseLoad(maxAgeMillis=$maxAgeMillis)"
        }
    }

    public companion object {

        @JvmField
        public val ReloadRevalidatingCacheData: AdaptyPlacementFetchPolicy = AdaptyPlacementFetchPolicy.ReloadRevalidatingCacheData.create()

        @JvmField
        public val ReturnCacheDataElseLoad: AdaptyPlacementFetchPolicy = AdaptyPlacementFetchPolicy.ReturnCacheDataElseLoad.create()

        @JvmStatic
        public fun ReturnCacheDataIfNotExpiredElseLoad(maxAgeMillis: Long): AdaptyPlacementFetchPolicy = ReturnCacheDataIfNotExpiredElseLoad.create(maxAgeMillis)

        @JvmField
        public val Default: AdaptyPlacementFetchPolicy = ReloadRevalidatingCacheData
    }
}