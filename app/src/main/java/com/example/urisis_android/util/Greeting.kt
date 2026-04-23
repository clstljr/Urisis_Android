package com.example.urisis_android.util

import java.util.Calendar

object Greeting {
    fun forNow(now: Calendar = Calendar.getInstance()): String =
        when (now.get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "GOOD MORNING"
            in 12..17 -> "GOOD AFTERNOON"
            in 18..21 -> "GOOD EVENING"
            else      -> "GOOD NIGHT"
        }
}