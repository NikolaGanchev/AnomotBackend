package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.AppealAction
import com.anomot.anomotbackend.utils.AppealObjective
import com.anomot.anomotbackend.utils.AppealReason
import java.util.*

data class AdminAppealDto(
        val appealedBy: UserDto,
        val reason: AppealReason,
        val objective: AppealObjective,
        val media: MediaDto,
        val similarPosts: List<PostDto>?,
        val decided: Boolean,
        val decidedBy: UserDto?,
        val decision: AppealAction?,
        val explanation: String?,
        var creationDate: Date,
        val id: String
)
