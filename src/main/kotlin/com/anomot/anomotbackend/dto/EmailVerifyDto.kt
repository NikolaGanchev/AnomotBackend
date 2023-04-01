package com.anomot.anomotbackend.dto

import javax.validation.constraints.Size

data class EmailVerifyDto(
        @Size(max = 36, min = 36)
        val verificationCode: String)