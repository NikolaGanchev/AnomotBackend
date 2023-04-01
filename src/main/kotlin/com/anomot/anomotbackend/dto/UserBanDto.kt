package com.anomot.anomotbackend.dto

import java.util.Date
import javax.validation.constraints.Size
import javax.validation.constraints.NotNull

data class UserBanDto(
        val userId: String,
        @Size(min = 1, max = 2000)
        @NotNull
        val reason: String,
        val until: Date
)
