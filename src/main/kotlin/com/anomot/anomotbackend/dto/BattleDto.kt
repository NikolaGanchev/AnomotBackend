package com.anomot.anomotbackend.dto

data class BattleDto(
        val goldPost: BattlePostDto,
        val redPost: BattlePostDto,
        val voteJWT: String,
        val id: String
)
