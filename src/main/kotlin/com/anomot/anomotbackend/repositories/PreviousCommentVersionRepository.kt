package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Comment
import com.anomot.anomotbackend.entities.PreviousCommentVersion
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PreviousCommentVersionRepository: JpaRepository<PreviousCommentVersion, Long> {

    fun findByComment(comment: Comment, pageable: Pageable): List<PreviousCommentVersion>

    fun deleteAllByComment(comment: Comment): Long

    @Modifying
    @Query("delete from PreviousCommentVersion cv where cv.comment.id in (select c.id from Comment c where c.commenter = ?1)")
    fun deleteByUser(user: User)
}