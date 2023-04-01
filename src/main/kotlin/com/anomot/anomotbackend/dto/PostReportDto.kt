package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.PostReportReason
import javax.validation.constraints.Size

data class PostReportDto(
        val reason: PostReportReason,
        val postId: String,
        @Size(min = 0, max = 1000)
        val other: String?
)