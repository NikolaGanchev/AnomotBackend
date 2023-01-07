package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.VotedBattleIntermediate
import com.anomot.anomotbackend.entities.Battle
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.entities.Vote
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VoteRepository: JpaRepository<Vote, Long> {
    fun countVotesByBattle(battle: Battle): Long
    fun countVotesByBattleAndPost(battle: Battle, post: Post): Long

    @Query("select new com.anomot.anomotbackend.dto.VotedBattleIntermediate(v, " +
            "(select count (v1) from Vote v1 where v1.battle = v.battle and v1.post = v.post), " +
            "(select count (v2) from Vote v2 where v2.battle = v.battle and v2.post <> v.post)) from Vote v where v.voter = ?1")
    fun getAllByVoter(voter: User, pageable: Pageable): List<VotedBattleIntermediate>

}