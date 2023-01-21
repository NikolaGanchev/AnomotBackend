package com.anomot.anomotbackend.dto

import javax.validation.constraints.Size

data class DecisionDto(
        val reportId: String,
        @Size(min = 1, max = 15_000)
        val decision: String
)
