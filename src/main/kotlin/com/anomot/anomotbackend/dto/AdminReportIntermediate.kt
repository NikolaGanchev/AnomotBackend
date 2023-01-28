package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.utils.ReportReason

data class AdminReportIntermediate(
        val reportReason: ReportReason,
        val other: String?,
        val reporter: User?,
        val ticketId: Long
)
