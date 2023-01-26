package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.AppealDecision
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AppealDecisionRepository: JpaRepository<AppealDecision, Long> {

    @Query("update AppealDecision ad set ad.decidedBy = NULL where ad.decidedBy = ?1 ")
    @Modifying
    fun setNullByUser(user: User)
}