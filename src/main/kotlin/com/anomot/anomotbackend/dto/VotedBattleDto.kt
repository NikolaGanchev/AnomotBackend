package com.anomot.anomotbackend.dto

data class VotedBattleDto(
        val votedPost: PostDto?,
        val otherPost: BattlePostDto?,
        val votesForVoted: Long,
        val votesForOther: Long,
        val otherUserDto: UserDto?,
        val isFinished: Boolean
)
