package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.CommentReportReason
import javax.validation.constraints.Size

data class CommentReportDto(
        val reason: CommentReportReason,
        val commentId: String,
        @Size(min = 0, max = 1000)
        val other: String?
)
