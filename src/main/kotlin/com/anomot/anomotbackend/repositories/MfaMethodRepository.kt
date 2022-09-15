package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.MfaMethod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MfaMethodRepository: JpaRepository<MfaMethod, Long> {
    fun findByMethod(method: String): MfaMethod
}