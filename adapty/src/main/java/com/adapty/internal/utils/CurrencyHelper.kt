package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.thread

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CurrencyHelper {

    private val currencyMap = hashMapOf<String, Currency>()

    private val lock = ReentrantReadWriteLock()

    private var currencyLocaleMap = hashMapOf<Currency, Locale>()
        get() = try {
            lock.readLock().lock()
            field
        } finally {
            lock.readLock().unlock()
        }

    init {
        fillCurrencyLocaleMap()
    }

    @JvmSynthetic
    fun getCurrencySymbol(currencyCode: String) =
        currencyMap.getOrPut(currencyCode, { Currency.getInstance(currencyCode) })
            .getOnlySymbol() ?: currencyCode

    private fun Currency.getOnlySymbol(): String? {
        if (!currencyLocaleMap.containsKey(this)) return null

        val rawSign = getSymbol(currencyLocaleMap[this])
        return rawSign.firstOrNull { char -> char !in CharRange('A', 'Z') }?.toString() ?: rawSign
    }

    private fun fillCurrencyLocaleMap() {
        thread {
            try {
                lock.writeLock().lock()
                Locale.getAvailableLocales().forEach { locale ->
                    try {
                        currencyLocaleMap[Currency.getInstance(locale)] = locale
                    } catch (e: Exception) {
                    }
                }
            } finally {
                lock.writeLock().unlock()
            }
        }
    }
}