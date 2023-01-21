package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.ReportType
import java.util.*

data class AdminReportDto(
        val reasons: Array<SingleReportDto>,
        val reportType: ReportType,
        val reporterId: String?,
        val postId: String?,
        val battleId: String?,
        val commentId: String?,
        val userId: String?,
        val decided: Boolean = false,
        val decision: String? = null,
        val decidedById: String? = null,
        val decidedOn: Date? = null,
        var creationDate: Date?,
        val reportId: String
)
