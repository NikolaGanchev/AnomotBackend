package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.ReportReason
import com.anomot.anomotbackend.utils.ReportType

data class SingleReportDto(
        val reportReason: ReportReason,
        val other: String?
)

data class ReportDto(
        val reasons: Array<SingleReportDto>,
        val reportType: ReportType,
)
