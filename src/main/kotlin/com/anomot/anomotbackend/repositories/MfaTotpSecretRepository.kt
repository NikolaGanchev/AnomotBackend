package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.MfaTotpSecret
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MfaTotpSecretRepository: JpaRepository<MfaTotpSecret, Long> {
    @Query("select mfaTotpSecret from MfaTotpSecret mfaTotpSecret where mfaTotpSecret.user.email = ?1")
    fun findByEmail(email: String): MfaTotpSecret?
}