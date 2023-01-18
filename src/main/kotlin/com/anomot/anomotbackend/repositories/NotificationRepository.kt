package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Notification
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository: JpaRepository<Notification, Long> {
    fun findAllByUser(user: User, pageable: Pageable): List<Notification>

    @Query("update Notification n set n.isRead = ?3 where n.id = ?2 and n.user = ?1")
    @Modifying
    fun setReadByUserAndId(user: User, id: Long, isRead: Boolean): Int

    @Query("update Notification n set n.isRead = ?3 where cast(n.id as long) in ?2 and n.user = ?1")
    @Modifying
    fun setReadByUserAndIds(user: User, ids: List<Long>, isRead: Boolean): Int

    @Modifying
    @Query("delete from Notification n where n.user = ?1")
    fun deleteByUser(user: User)
}