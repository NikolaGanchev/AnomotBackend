package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Appeal
import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AppealRepository: JpaRepository<Appeal, Long> {
    fun getAllByAppealedBy(appealedBy: User, pageable: Pageable): List<Appeal>

    @Query("select a from Appeal a")
    fun getAll(pageable: Pageable): List<Appeal>

    fun getAllByDecidedIsFalse(pageable: Pageable): List<Appeal>

    @Query("delete from Appeal a where a.media = ?1")
    @Modifying
    fun deleteByMedia(media: Media)

    @Query("delete from Appeal a where a.appealedBy = ?1")
    @Modifying
    fun deleteByUser(user: User)
}