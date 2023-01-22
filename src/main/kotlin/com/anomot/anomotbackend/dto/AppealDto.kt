package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.AppealObjective
import com.anomot.anomotbackend.utils.AppealReason

data class AppealDto(
        val reason: AppealReason,
        val objective: AppealObjective,
        val mediaId: String
)
