package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.AdminBattleIntermediate
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
            "(select count (v1) from Vote v1 where v1.battle = b and v1.post.poster = ?1)," +
            "(select count (v2) from Vote v2 where v2.battle = b and v2.post.poster <> ?1))" +
            "from Battle b where b.goldPost.poster = ?1 or b.redPost.poster = ?1")
    fun getAllBattlesByUser(user: User, pageable: Pageable): List<BattleIntermediate>

    @Query("select new com.anomot.anomotbackend.dto.BattleIntermediate(b, " +
            "(select count (v1) from Vote v1 where v1.battle = b and v1.post.poster = ?1)," +
            "(select count (v2) from Vote v2 where v2.battle = b and v2.post.poster <> ?1))" +
            "from Battle b where b.id = ?2 and (b.goldPost.poster = ?1 or b.redPost.poster = ?1)")
    fun getBattleByUser(user: User, id: Long): BattleIntermediate?

    @Query("select new com.anomot.anomotbackend.dto.AdminBattleIntermediate(b, " +
            "(select count (v1) from Vote v1 where v1.battle = b and v1.post = b.goldPost)," +
            "(select count (v2) from Vote v2 where v2.battle = b and v2.post = b.redPost))" +
            "from Battle b where b.id = ?1")
    fun getBattleById(id: Long): AdminBattleIntermediate

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

    @Query("select distinct p from Post p where " +
            "(p.id in (select b.goldPost.id from Battle b where b.goldPost = p) or " +
            "p.id in (select b.redPost.id from Battle b where b.redPost = p) or " +
            "p.id in (select bp.post.id from BattleQueuePost bp where bp.post = p)) " +
            "and p.poster = :user and " +
            // check for same media type
            "p.media.mediaType = :#{#media.mediaType} and " +
            // check for duration within 2 second if possible
            "abs(coalesce(p.media.duration, 0) - :duration) < 2 and " +
            "function('hamming_distance', p.media.phash, :#{#media.phash}) < 6")
    fun getSimilarMedia(user: User, media: Media, duration: Float?): List<Post>

    @Query("select p from Post p where " +
            "p.id in (select b.goldPost.id from Battle b where b.goldPost.poster = ?1 and b.goldPost.text = ?2) or " +
            "p.id in (select b.redPost.id from Battle b where b.redPost.poster = ?1 and b.redPost.text = ?2) or " +
            "p.id in (select bp.post.id from BattleQueuePost bp where bp.post.poster = ?1 and bp.post.text = ?2)")
    fun getWithSameText(user: User, text: String): List<Post>

    @Query("select count(b.id) > 0 " +
            "from Battle b where b = ?2 and ((b.goldPost.poster = ?1 or b.redPost.poster = ?1) or b.id in (select v.battle.id from Vote v where v.battle = ?2 and v.voter = ?1))")
    fun canSeeBattle(user: User, battle: Battle): Boolean

    @Query("update Battle b set " +
            "b.goldPost = case when (b.goldPost = ?1) then NULL else b.goldPost end, " +
            "b.redPost = case when(b.redPost = ?1) then NULL else b.redPost end where b.redPost = ?1 or b.goldPost = ?1")
    @Modifying
    fun setPostToNull(post: Post): Int

    @Query("select count(b) > 0 from Battle b where b.redPost = ?1 or b.goldPost = ?1")
    fun existsByRedPostOrGoldPost(post: Post): Boolean

    @Modifying
    @Query("update Battle b set " +
            "b.goldPost = case when (b.goldPost.id in (select p.id from Post p where p.poster = ?1)) then NULL else b.goldPost end, " +
            "b.redPost = case when(b.redPost.id in (select p.id from Post p where p.poster = ?1)) then NULL else b.redPost end " +
            "where b.redPost.id in (select p.id from Post p where p.poster = ?1) or b.goldPost.id in (select p.id from Post p where p.poster = ?1)")
    fun setPostsByUserToNull(user: User)

    @Query("select b.id from Battle b where b.goldPost is null and b.redPost is null")
    fun getDangling(): List<Long>

    @Query("from Battle b where b.redPost = ?1 or b.goldPost = ?1")
    fun getByRedPostOrGoldPost(post: Post): Battle?
}