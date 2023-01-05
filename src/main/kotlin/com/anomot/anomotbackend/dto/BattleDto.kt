package com.anomot.anomotbackend.dto

import java.util.Date

data class BattleDto(
        val userPost: BattlePostDto,
        val enemyPost: BattlePostDto,
        val until: Date
)
