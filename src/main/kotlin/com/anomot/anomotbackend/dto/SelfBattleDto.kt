package com.anomot.anomotbackend.dto

import java.util.Date

data class SelfBattleDto(
        val goldPost: BattlePostDto,
        val redPost: BattlePostDto,
        val goldVotes: Int,
        val redVotes: Int,
        val isFinished: Boolean = false,
        val until: Date
)
