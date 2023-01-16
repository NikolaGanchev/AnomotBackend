package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Notification
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository: JpaRepository<Notification, Long> {
    fun findAllByUser(user: User, pageable: Pageable): List<Notification>
}