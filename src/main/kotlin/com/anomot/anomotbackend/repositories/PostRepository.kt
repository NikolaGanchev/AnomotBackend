package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PostRepository: JpaRepository<Post, Long> {

    fun findAllByPoster(poster: User, pageable: Pageable): List<Post>

    fun deleteByIdAndPoster(id: Long, poster: User): Long

    @Query("with followed as (select followed_id from follow where follower_id = ?1) " +
            "select * from post where exists(select 1 from followed where poster_id = followed_id)", nativeQuery = true)
    fun getFeed(userId: Long, pageable: Pageable): List<Post>
}