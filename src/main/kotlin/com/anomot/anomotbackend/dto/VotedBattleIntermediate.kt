package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.entities.Vote

data class VotedBattleIntermediate(
        val vote: Vote,
        val votesForVoted: Long,
        val votesForOther: Long,
        val canSeeOtherUser: Boolean)
