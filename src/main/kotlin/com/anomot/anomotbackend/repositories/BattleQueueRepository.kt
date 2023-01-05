package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.BattleQueuePost
import com.anomot.anomotbackend.utils.Constants
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BattleQueueRepository: JpaRepository<BattleQueuePost, Long> {
    @Query("from BattleQueuePost p where p.post.type = :#{#post.post.type} " +
            "and p.post <> :#{#post.post} " +
            "and p.post.poster <> :#{#post.post.poster} " +
            "and abs(p.post.poster.elo - :#{#post.post.poster.elo}) < ${Constants.MAX_ELO_DIFFERENCE} " +
            "order by abs(p.post.poster.elo - :#{#post.post.poster.elo}) asc")
    fun findSimilarByElo(@Param("post") post: BattleQueuePost, pageable: Pageable =
        PageRequest.of(0, 1)): List<BattleQueuePost>
}