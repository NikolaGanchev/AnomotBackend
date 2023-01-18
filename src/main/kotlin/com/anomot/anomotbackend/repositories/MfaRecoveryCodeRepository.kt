package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.MfaRecoveryCode
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MfaRecoveryCodeRepository: JpaRepository<MfaRecoveryCode, Long> {
    fun deleteAllByUser(user: User): Long

    fun getAllByUser(user: User): List<MfaRecoveryCode>?

    @Modifying
    @Query("delete from MfaRecoveryCode c where c.user = ?1")
    fun deleteByUser(user: User)
}