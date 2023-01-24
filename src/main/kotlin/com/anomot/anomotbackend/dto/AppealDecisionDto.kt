package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.AppealAction

data class AppealDecisionDto(
        val id: String,
        val decision: AppealAction,
        val explanation: String
)