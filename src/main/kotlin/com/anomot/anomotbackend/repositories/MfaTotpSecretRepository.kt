package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.MfaTotpSecret
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MfaTotpSecretRepository: JpaRepository<MfaTotpSecret, Long> {
    @Query("select mfaTotpSecret from MfaTotpSecret mfaTotpSecret where mfaTotpSecret.user.email = ?1")
    fun findByEmail(email: String): MfaTotpSecret?

    @Modifying
    @Query("delete from MfaTotpSecret mfaTotpSecret where mfaTotpSecret.user.id = ?1")
    fun deleteByUserId(id: Long)

    @Modifying
    @Query("delete from MfaTotpSecret s where s.user = ?1")
    fun deleteByUser(user: User)
}