package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.Battle

data class BattleIntermediate(
        val battle: Battle,
        val votesForSelf: Long,
        val votesForOther: Long
)
