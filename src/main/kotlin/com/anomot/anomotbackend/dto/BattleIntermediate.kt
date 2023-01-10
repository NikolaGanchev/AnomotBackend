package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.Battle

data class BattleIntermediate(
        val battle: Battle,
        val votesForSelf: Long,
        val votesForOther: Long,
        val goldPostLikes: Long,
        val redPostLikes: Long,
        val hasLikedGoldPost: Boolean,
        val hasLikedRedPost: Boolean
)
