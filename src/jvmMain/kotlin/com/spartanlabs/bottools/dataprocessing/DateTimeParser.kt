package com.spartanlabs.bottools.dataprocessing

class DateTimeParser {
    fun readDateMMDD(rawDate: String): Date {
        val month = rawDate.substring(0, 2)
        val date = rawDate.substring(3, 5)
        return readDate(date, month)
    }

    fun readDateDDMM(rawDate: String) = readDate(
        month = rawDate.substring(3, 5),
        date = rawDate.substring(0, 2)
    )

    private fun readDate(date: String, month: String): Date {
        return try {
            Date(date.toInt(), month.toInt())
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Date needs to be in format: mm/dd")
        }
    }
}