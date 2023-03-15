package com.anomot.anomotbackend.dto

import jakarta.validation.constraints.Size

data class AppealUploadDto(
        @Size(min = 1, max = 10000)
        val jwt: String
)