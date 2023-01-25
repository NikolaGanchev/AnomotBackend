package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.ReportType
import java.util.*

data class ReportTicketDto(
        val reportType: ReportType,
        val post: PostDto?,
        val battle: AdminBattleDto?,
        val commentId: CommentDto?,
        val userId: UserDto?,
        val isDecided: Boolean,
        val decisions: Int,
        val creationDate: Date,
        val id: String,
)
