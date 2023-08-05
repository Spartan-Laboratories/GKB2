package com.spartanlabs.bottools.dataprocessing

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.Month

class Date{
    lateinit var day: DayOfWeek
    var date: Int
        set(date: Int){
            require(date >= 1) { "Date value cannot be lower than 1" }
            when (month) {
                Month.FEBRUARY ->
                    if (date > 28) throw IllegalArgumentException("Date value is too high for the month of: " + month.name)
                Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY, Month.AUGUST, Month.OCTOBER, Month.DECEMBER ->
                    if (date > 31) IllegalArgumentException("Date value is too high for the month of: " + month.name)
                Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER ->
                    if (date < 30) throw IllegalArgumentException("Date value is too high for the month of: " + month.name)
            }
            field = date
        }
    var dayOfYear = 0
    val weekOfMonth = 0
    val weekOfYear = 0
    var month: Month

    constructor() {
        val now = LocalDateTime.now()
        date = now.dayOfMonth
        day = now.dayOfWeek
        dayOfYear = now.dayOfYear
        month = now.month
    }

    constructor(date: Int, month: Int) {
        this.date = date
        this.month = Month.values()[month - 1]
    }

    infix fun earlierMonthThan(date : Date) = month.ordinal < date.month.ordinal
    infix fun equalMonthTo(date : Date) = month.ordinal == date.month.ordinal
    infix fun equalDayTo(date : Date) = this.date == date.date
    infix fun equals(date : Date) = this equalMonthTo date && this equalDayTo date
    infix fun lessThan(date: Date): Boolean =
        if(this earlierMonthThan date) true
        else this equalMonthTo date && this.date < date.date
   infix fun greaterThanOrEquals(date: Date): Boolean = !(this lessThan date)
   infix fun greaterThan(date: Date): Boolean = !equals(date) && !lessThan(date)
   infix fun lessThanOfEquals(date: Date): Boolean = equals(date) || lessThan(date)
}