package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Comment
import com.anomot.anomotbackend.entities.PreviousCommentVersion
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PreviousCommentVersionRepository: JpaRepository<PreviousCommentVersion, Long> {

    fun findByComment(comment: Comment, pageable: Pageable): List<PreviousCommentVersion>

    fun deleteAllByComment(comment: Comment): Long
}