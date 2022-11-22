package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.MfaRecoveryCode
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MfaRecoveryCodeRepository: JpaRepository<MfaRecoveryCode, Long> {
    fun deleteAllByUser(user: User): Long

    fun getAllByUser(user: User): List<MfaRecoveryCode>?
}