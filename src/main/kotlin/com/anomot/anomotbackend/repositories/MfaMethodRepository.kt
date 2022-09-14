package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.MfaMethod
import org.springframework.data.jpa.repository.JpaRepository

interface MfaMethodRepository: JpaRepository<MfaMethod, Long> {
    fun findByMethod(method: String): MfaMethod
}