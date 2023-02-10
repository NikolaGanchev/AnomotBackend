package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Comment
import com.anomot.anomotbackend.entities.CommentLike
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CommentLikeRepository: JpaRepository<CommentLike, Long> {
    fun existsByLikedByAndComment(likedBy: User, comment: Comment): Boolean

    fun deleteByLikedByAndComment(likedBy: User, comment: Comment): Long

    @Query("select distinct(l.likedBy) from CommentLike l, Follow f where l.comment = ?2 and (l.likedBy = ?1 or (f.followed = l.likedBy and f.follower = ?1))")
    fun getLikedByByUserAndComment(user: User, comment: Comment, pageable: Pageable): List<User>

    @Query("select distinct(l.likedBy) from CommentLike l where l.comment = ?1")
    fun getLikedByByComment(comment: Comment, pageable: Pageable): List<User>

    @Modifying
    @Query("delete from CommentLike l where l.likedBy = ?1")
    fun deleteByUser(user: User)
    fun deleteByComment(comment: Comment)
}