package com.adapty.models

/**
 * @param year,
 * @param month,
 * @param date correspond to their ordinal numbers in a regular calendar,
 * month and date numbers start with 1.
 * For example, Date(year = 1970, month = 1, date = 3) represents January 3, 1970.
 */
public class Date(
    private val year: Int,
    private val month: Int,
    private val date: Int
) {
    override fun toString(): String {
        return "$year-${String.format("%02d", month)}-${String.format("%02d", date)}"
    }
}