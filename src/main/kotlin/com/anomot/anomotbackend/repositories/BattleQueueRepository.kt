package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.PostWithLikes
import com.anomot.anomotbackend.entities.BattleQueuePost
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.utils.Constants
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.Tuple

@Repository
interface BattleQueueRepository: JpaRepository<BattleQueuePost, Long> {
    @Query("select bp.* from battle_queue_post bp join post p on bp.post_id = p.id join users u on p.poster_id = u.id " +
            "where p.type = :#{#post.post.type.ordinal()} " +
            "and p.id <> :#{#post.post.id} " +
            "and p.poster_id <> :#{#post.post.poster.id} " +
            "and abs(u.elo - :#{#post.post.poster.elo}) < case " +
            "when (extract(epoch from now() - bp.creation_date) < 60) then ${Constants.MAX_ELO_1_MINUTE} " +
            "when (extract(epoch from now() - bp.creation_date) < 60 * 5) then ${Constants.MAX_ELO_5_MINUTES} " +
            "when (extract(epoch from now() - bp.creation_date) < 60 * 60) then ${Constants.MAX_ELO_1_HOUR} " +
            "else ${Constants.MAX_ELO_DIFFERENCE} end " +
            "order by abs(u.elo - :#{#post.post.poster.elo}) asc, bp.creation_date asc", nativeQuery = true)
    fun findSimilarByElo(@Param("post") post: BattleQueuePost, pageable: Pageable =
        PageRequest.of(0, 1)): List<BattleQueuePost>

    @Query("with r as (select distinct on (bp.id) bp.id as id1, bp1.id as id2 from battle_queue_post bp join post p on bp.post_id = p.id join users u on p.poster_id = u.id, " +
            " battle_queue_post bp1 join post p1 on bp1.post_id = p1.id join users u1 on p1.poster_id = u1.id " +
            "where p.type = p1.type " +
            "and bp.id < bp1.id " +
            "and p.id <> p1.id " +
            "and p.poster_id <> p1.poster_id " +
            "and abs(u.elo - u1.elo) < case " +
            "when (extract(epoch from now() - bp.creation_date) < 60) then ${Constants.MAX_ELO_1_MINUTE} " +
            "when (extract(epoch from now() - bp.creation_date) < 60 * 5) then ${Constants.MAX_ELO_5_MINUTES} " +
            "when (extract(epoch from now() - bp.creation_date) < 60 * 60) then ${Constants.MAX_ELO_1_HOUR} " +
            "else ${Constants.MAX_ELO_DIFFERENCE} end " +
            " order by bp.id, abs(u.elo - u1.elo) asc, bp.creation_date asc) " +
            "select distinct on (r.id2) r.id1, r.id2 from r order by r.id2", nativeQuery = true)
    fun matchmake(): List<Tuple>

    @Query("delete from BattleQueuePost p where p.post.id = ?1 and p.post.id in (select p1.id from Post p1 where p1.poster.id = ?2)")
    @Modifying
    fun deletePostByIdAndUser(postId: Long, userId: Long): Int

    @Query("select new com.anomot.anomotbackend.dto.PostWithLikes(p.post, " +
            "(select count(l) from Like l where l.post = p.post), " +
            "(select count(l) > 0 from Like l where l.likedBy = ?2)) " +
            "from BattleQueuePost p where p.post.poster = ?1")
    fun getAllByPostPoster(poster: User, requester: User, pageable: Pageable): List<PostWithLikes>

    @Modifying
    @Query("delete from BattleQueuePost p where p.post.id in (select p.id from Post p where p.poster = ?1)")
    fun deleteByUser(user: User)

    @Query("select count(b) from BattleQueuePost b where b.creationDate > ?1")
    fun findByAfterDate(from: Date): Long

    @Query("from BattleQueuePost post")
    fun getAll(): List<BattleQueuePost>
}