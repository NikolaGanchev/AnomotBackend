package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Authority
import org.springframework.data.jpa.repository.JpaRepository

interface AuthorityRepository: JpaRepository<Authority, Long> {
    fun findByAuthority(authority: String): Authority?
}