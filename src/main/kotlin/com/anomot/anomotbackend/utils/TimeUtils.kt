package com.anomot.anomotbackend.utils

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class TimeUtils {
    companion object {
        fun generateFutureDate(after: Int): Date {
            val expiryDate = OffsetDateTime.now( ZoneOffset.UTC )
                    .plusDays(after.toLong())
            return Date.from(expiryDate.toInstant())
        }

        fun generateFutureAfterMinutes(after: Int): Date {
            val expiryDate = OffsetDateTime.now( ZoneOffset.UTC )
                    .plusMinutes(after.toLong())
            return Date.from(expiryDate.toInstant())
        }

        fun generatePastMinutesAgo(before: Int): Date {
            val expiryDate = OffsetDateTime.now( ZoneOffset.UTC )
                    .minusMinutes(before.toLong())
            return Date.from(expiryDate.toInstant())
        }
    }
}