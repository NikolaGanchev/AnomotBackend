package com.anomot.anomotbackend.dto

import java.util.*

data class TicketDecisionDto(
        val decision: String,
        val decidedBy: UserDto?,
        var creationDate: Date,
)