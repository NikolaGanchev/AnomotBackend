package com.anomot.anomotbackend.dto

import java.util.Date
import jakarta.validation.constraints.Size
import org.jetbrains.annotations.NotNull

data class UserBanDto(
        val userId: String,
        @Size(min = 1, max = 2000)
        @NotNull
        val reason: String,
        val until: Date
)
