package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.Battle

data class AdminBattleIntermediate(
        val battle: Battle,
        val votesForGold: Long,
        val votesForRed: Long)
