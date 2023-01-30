package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.AverageVotePossibilitiesToActualVotesDto
import com.anomot.anomotbackend.dto.VotedBattleIntermediate
import com.anomot.anomotbackend.entities.Battle
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.entities.Vote
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VoteRepository: JpaRepository<Vote, Long> {
    fun countVotesByBattle(battle: Battle): Long
    fun countVotesByBattleAndPost(battle: Battle, post: Post): Long

    @Query("select new com.anomot.anomotbackend.dto.VotedBattleIntermediate(v, " +
            "(select count (v1) from Vote v1 where v1.battle = v.battle and v1.post = v.post), " +
            "(select count (v2) from Vote v2 where v2.battle = v.battle and v2.post <> v.post)," +
            // Can see other user
            "(select count(b)>0 from Battle b, Vote v where " +
            "((b.redPost.poster = ?1 and b.goldPost.poster = v.battle.goldPost.poster) or " +
            "(b.goldPost.poster = ?1 and b.redPost.poster = v.battle.redPost.poster)) or " +
            "(exists(from Vote v where v.voter = ?1 and v.post.poster = b.goldPost.poster) " +
            "and exists(from Vote v where v.voter = ?1 and v.post.poster = b.redPost.poster)))) "+
            "from Vote v where v.voter = ?1")
    fun getAllByVoter(voter: User, pageable: Pageable): List<VotedBattleIntermediate>

    @Query("select new com.anomot.anomotbackend.dto.VotedBattleIntermediate(v, " +
            "(select count (v1) from Vote v1 where v1.battle = v.battle and v1.post = v.post), " +
            "(select count (v2) from Vote v2 where v2.battle = v.battle and v2.post <> v.post)," +
            // Can see other user
            "(select count(b)>0 from Battle b, Vote v where " +
            "((b.redPost.poster = ?1 and b.goldPost.poster = :#{#battle.goldPost.poster}) or " +
            "(b.goldPost.poster = ?1 and b.redPost.poster = :#{#battle.redPost.poster})) or " +
            "(exists(from Vote v where v.voter = ?1 and v.post.poster = b.goldPost.poster) " +
            "and exists(from Vote v where v.voter = ?1 and v.post.poster = b.redPost.poster)))) "+
            "from Vote v where v.voter = ?1 and v.battle = ?2")
    fun getByVoterAndBattle(voter: User, @Param("battle") battle: Battle): VotedBattleIntermediate

    fun existsByBattleAndVoter(battle: Battle, user: User): Boolean

    @Query("update Vote v set v.post = NULL where v.post = ?1")
    @Modifying
    fun setPostToNull(post: Post)

    @Modifying
    @Query("delete from Vote v where v.voter = ?1")
    fun deleteByUser(user: User)

    fun findAllByBattleAndPost(battle: Battle, post: Post): List<Vote>

    @Modifying
    @Query("delete from Vote v where v.battle = ?1")
    fun deleteByBattle(battles: Battle)

    @Query("select (select avg(b.total_vote_possibilities) from Battle b where b.creation_date > ?1) as averageVotePossibilities, " +
            "avg(last_count) as averageActualVotes from " +
            "(select count(v.id) as last_count from Vote v where v.creation_date > ?1 group by v.battle_id) as vlc", nativeQuery = true)
    fun getAverageVotePossibilitiesToActualVotes(from: Date): AverageVotePossibilitiesToActualVotesDto
}