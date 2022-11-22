package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.RememberMeToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RememberMeTokenRepository: JpaRepository<RememberMeToken, Long> {
    fun findBySeries(series: String): RememberMeToken?

    @Modifying
    @Query("update RememberMeToken token set token.tokenValue = ?2, token.date = ?3 where token.series = ?1")
    fun updateBySeries(series: String, tokenValue: String, lastUsed: Date): Int

    fun deleteAllByEmail(email: String): Int

    @Modifying
    @Query("delete from RememberMeToken token where token.date < ?1")
    fun deleteOldTokens(expiry: Date): Int
}