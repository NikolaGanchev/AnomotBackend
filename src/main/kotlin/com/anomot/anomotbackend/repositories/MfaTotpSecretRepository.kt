package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.MfaTotpSecret
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MfaTotpSecretRepository: JpaRepository<MfaTotpSecret, Long> {
    fun findByUser(user: User): MfaTotpSecret
}