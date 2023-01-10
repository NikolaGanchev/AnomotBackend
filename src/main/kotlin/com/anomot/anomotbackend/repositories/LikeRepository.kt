package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Like
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface LikeRepository: JpaRepository<Like, Long> {

    fun deleteByLikedByAndPostPoster(likedBy: User, postPoster: User): Long

    fun existsByLikedByAndPost(likedBy: User, post: Post): Boolean

    fun deleteByLikedByAndPost(likedBy: User, post: Post): Long

    @Query("select distinct(l.likedBy) from Like l, Follow f where l.post = ?2 and (l.likedBy = ?1 or (f.followed = l.likedBy and f.follower = ?1))")
    fun getLikedByByUserAndPost(user: User, post: Post, pageable: Pageable): List<User>
}