package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.SuccessfulLogin
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SuccessfulLoginRepository: JpaRepository<SuccessfulLogin, Long> {
    fun findAllByUser(user: User, pageable: Pageable): List<SuccessfulLogin>

    fun findByUserAndId(user: User, id: Long): SuccessfulLogin?

    @Modifying
    @Query("delete from SuccessfulLogin login where login.user = ?1")
    fun deleteByUser(user: User)

    @Query("select count(l) from SuccessfulLogin l where l.date > ?1")
    fun findByAfterDate(from: Date): Long
}