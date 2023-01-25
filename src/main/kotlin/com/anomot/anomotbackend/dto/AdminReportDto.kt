package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.ReportReason

data class AdminReportDto(
        val reportReason: ReportReason,
        val other: String?,
        val reporter: UserDto?
)