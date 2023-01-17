package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.BattleIntermediate
import com.anomot.anomotbackend.entities.Battle
import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BattleRepository: JpaRepository<Battle, Long> {

    @Query("update battle set finished = true where (finish_date < now() and finished = false) " + // select if battle finished normally
            // end early if one of the posts is deleted
            "or (finished = false and (not exists(select * from post where post.id = battle.red_post_id) or not exists(select * from post where post.id = battle.gold_post_id))) " +
            "returning *",
            nativeQuery = true)
    @Modifying
    fun getUnprocessedFinishedBattlesAndUpdate(): List<Battle>

    @Query("select new com.anomot.anomotbackend.dto.BattleIntermediate(b, " +
            "(select count (v1) from Vote v1 where v1.battle = b and v1.voteFor = ?1)," +
            "(select count (v2) from Vote v2 where v2.battle = b and v2.voteFor <> ?1))" +
            "from Battle b where b.goldPost.poster = ?1 or b.redPost.poster = ?1")
    fun getAllBattlesByUser(user: User, pageable: Pageable): List<BattleIntermediate>

    @Query("select new com.anomot.anomotbackend.dto.BattleIntermediate(b, " +
            "(select count (v1) from Vote v1 where v1.battle = b and v1.voteFor = ?1)," +
            "(select count (v2) from Vote v2 where v2.battle = b and v2.voteFor <> ?1))" +
            "from Battle b where b.id = ?2 and (b.goldPost.poster = ?1 or b.redPost.poster = ?1)")
    fun getBattleByUser(user: User, id: Long): BattleIntermediate?

    @Query("select * from battle where " +
            // Ignore if you are the poster
            "not exists(select from post where id = battle.red_post_id and poster_id = ?1) and " +
            "not exists(select from post where id = battle.gold_post_id and poster_id = ?1) and " +
            // Ignore if one of the posts is deleted
            "exists(select from post where id = battle.gold_post_id) and " +
            "exists(select from post where id = battle.red_post_id) and " +
            // Ignore if you have already voted
            "not exists(select from vote where vote.battle_id = battle.id and voter_id = ?1)" +
            // Do not show 30 seconds before finish
            "and extract(epoch from (finish_date - now())) > 30", nativeQuery = true)
    fun getBattle(userId: Long, pageable: Pageable): List<Battle?>

    @Query("from Battle b where b.id = ?1 and b.finished = false")
    fun getByIdAndFinishedFalse(id: Long): Battle?

    @Query("select p from Post p, Battle b, BattleQueuePost bp where (b.redPost = p or b.goldPost = p or bp.post = p) " +
            "and p.poster = :user and " +
            // check for same media type
            "p.type = :#{#media.mediaType} and " +
            // check for duration within 2 second if possible
            "abs(isnull(p.media.duration, 0) - isnull(:#{#media.duration}, 0)) < 2 and " +
            "function('hamming_distance', p.media.phash, :#{#media.phash}) < 15")
    fun getSimilarMedia(user: User, media: Media): List<Post>

    @Query("select p from Post p, Battle b, BattleQueuePost bp where (b.redPost = p or b.goldPost = p or bp.post = p) and" +
            "((b.goldPost.poster = :user and b.goldPost.text = :text) or " +
            "(b.redPost.poster = :user and b.redPost.text = :text))")
    fun getWithSameText(user: User, text: String): List<Post>

    @Query("select case when (count(b) > 0 or count(v) > 0) then true else false end " +
            "from Battle b, Vote v where (v.battle = ?2 and v.voter = ?1) or (b = ?2 and (b.goldPost.poster = ?1 or b.redPost.poster = ?1))")
    fun canSeeBattle(user: User, battle: Battle): Boolean

    @Query("update Battle b set " +
            "b.goldPost = case when (b.goldPost = ?1) then NULL else b.goldPost end, " +
            "b.redPost = case when(b.redPost = ?1) then NULL else b.redPost end where b.redPost = ?1 or b.goldPost = ?1")
    @Modifying
    fun setPostToNull(post: Post): Int

    @Query("from Battle b where b.redPost = ?1 or b.goldPost = ?1")
    fun getByRedPostOrGoldPost(post: Post): Battle?
}