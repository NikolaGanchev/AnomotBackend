package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.AppealReason

data class AppealDto(
        val reason: AppealReason,
        val mediaId: String
)