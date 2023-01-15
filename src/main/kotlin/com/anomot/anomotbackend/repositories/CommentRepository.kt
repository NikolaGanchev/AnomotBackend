package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Battle
import com.anomot.anomotbackend.entities.Comment
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository: JpaRepository<Comment, Long> {
    fun getAllByParentPost(post: Post, pageable: Pageable): List<Comment>

    fun getAllByParentBattle(battle: Battle, pageable: Pageable): List<Comment>

    fun getAllByParentComment(comment: Comment, pageable: Pageable): List<Comment>

    fun existsByParentComment(comment: Comment): Boolean

    @Query("update Comment c set c.text = '', c.commenter = null, c.isDeleted = true, c.isEdited = false " +
            "where c.commenter = ?1 and c.id = ?2")
    @Modifying
    fun setDeleted(user: User, commentId: Long)

    fun deleteByCommenterAndParentPostPoster(commenter: User, poster: User): Long

    @Query("update Comment c set c.text = ?1, c.isEdited = true " +
            "where c.id = ?2")
    @Modifying
    fun edit(newText: String, commentId: Long)
}