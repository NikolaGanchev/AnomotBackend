package com.anomot.anomotbackend.dto

import jakarta.validation.constraints.Size

data class EmailVerifyDto(
        @Size(max = 36, min = 36)
        val verificationCode: String)