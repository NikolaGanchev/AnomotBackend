package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.SuccessfulLogin
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SuccessfulLoginRepository: JpaRepository<SuccessfulLogin, Long> {
    fun findAllByUser(user: User, pageable: Pageable): List<SuccessfulLogin>
}