package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.BattlePostReportReason
import javax.validation.constraints.Size

data class BattleReportDto(
        val reason: BattlePostReportReason,
        val postId: String,
        @Size(min = 0, max = 1000)
        val other: String?,
        val battleId: String
)