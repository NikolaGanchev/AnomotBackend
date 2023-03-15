package com.anomot.anomotbackend.dto

import jakarta.validation.constraints.Size

data class DecisionDto(
        val reportTicketId: String,
        @Size(min = 1, max = 15_000)
        val decision: String
)
