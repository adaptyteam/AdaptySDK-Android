package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun <P, L, R> Flow<P>.mapWithLocalPAL(
    getLocalPAL: () -> Flow<L>,
    transform: (P, L) -> R,
): Flow<R> =
    flatMapLatest { profile ->
        getLocalPAL().map { localPAL -> transform(profile, localPAL) }
    }
