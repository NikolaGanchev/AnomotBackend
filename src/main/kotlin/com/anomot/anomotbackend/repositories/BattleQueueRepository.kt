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

@Repository
interface BattleQueueRepository: JpaRepository<BattleQueuePost, Long> {
    @Query("select bp.* from battle_queue_post bp join post p on bp.post_id = p.id join users u on p.poster_id = u.id " +
            "where p.type = :#{#post.post.type.ordinal()} " +
            "and p.id <> :#{#post.post.id} " +
            "and p.poster_id <> :#{#post.post.poster.id} " +
            "and abs(u.elo - :#{#post.post.poster.elo}) < case " +
            // 1 minute
            "when (extract(epoch from now() - p.creation_date) < 60) then ${Constants.MAX_ELO_1_MINUTE} " +
            "when (extract(epoch from now() - p.creation_date) < 60 * 5) then ${Constants.MAX_ELO_5_MINUTES} " +
            "when (extract(epoch from now() - p.creation_date) < 60 * 60) then ${Constants.MAX_ELO_1_HOUR} " +
            "else ${Constants.MAX_ELO_DIFFERENCE} end " +
            "order by abs(u.elo - :#{#post.post.poster.elo}) asc, p.creation_date asc", nativeQuery = true)
    fun findSimilarByElo(@Param("post") post: BattleQueuePost, pageable: Pageable =
        PageRequest.of(0, 1)): List<BattleQueuePost>

    @Query("delete from BattleQueuePost p where p.post.id = ?1 and p.post.id in (select post.id from Post post where post.poster.id = ?2)")
    @Modifying
    fun deletePostByIdAndUser(postId: Long, userId: Long): Int

    @Query("select new com.anomot.anomotbackend.dto.PostWithLikes(p.post, " +
            "(select count(l) from Like l where l.post = p.post), " +
            "(select count(l) > 0 from Like l where l.likedBy = ?2)) " +
            "from BattleQueuePost p where p.post.poster = ?1")
    fun getAllByPostPoster(poster: User, requester: User, pageable: Pageable): List<PostWithLikes>
}