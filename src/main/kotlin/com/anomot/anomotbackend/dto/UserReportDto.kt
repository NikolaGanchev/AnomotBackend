package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.UserReportReason
import jakarta.validation.constraints.Size

data class UserReportDto(
        val reason: UserReportReason,
        val userId: String,
        @Size(min = 0, max = 1000)
        val other: String?
)