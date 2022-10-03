package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.MfaRecoveryCode
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository

interface MfaRecoveryCodeRepository: JpaRepository<MfaRecoveryCode, Long> {
    fun deleteAllByUser(user: User): Long

    fun getAllByUser(user: User): List<MfaRecoveryCode>?
}