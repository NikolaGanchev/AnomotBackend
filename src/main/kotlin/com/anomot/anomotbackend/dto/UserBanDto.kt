package com.anomot.anomotbackend.dto

import java.util.Date
import jakarta.validation.constraints.Size

data class UserBanDto(
        val userId: String,
        @Size(min = 1, max = 2000)
        val reason: String,
        val until: Date
)
