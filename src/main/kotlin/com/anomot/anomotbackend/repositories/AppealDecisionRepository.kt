package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.AppealDecision
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AppealDecisionRepository: JpaRepository<AppealDecision, Long> {

    @Query("delete from AppealDecision ad where ad.id in (select a.decision.id from Appeal a where a.appealedBy = ?1)")
    @Modifying
    fun deleteByUser(user: User)

    @Query("update AppealDecision ad set ad.decidedBy = NULL where ad.decidedBy = ?1 ")
    @Modifying
    fun setNullByUser(user: User)
}