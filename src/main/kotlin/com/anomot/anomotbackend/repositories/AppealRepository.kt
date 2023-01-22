package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Appeal
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AppealRepository: JpaRepository<Appeal, Long> {
    fun getAllByAppealedBy(pageable: Pageable): List<Appeal>
}