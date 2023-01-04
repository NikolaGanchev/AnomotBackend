package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Battle
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.Vote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VoteRepository: JpaRepository<Vote, Long> {
    fun countVotesByBattle(battle: Battle): Long
    fun countVotesByBattleAndPost(battle: Battle, post: Post): Long
}