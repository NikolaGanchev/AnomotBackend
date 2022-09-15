package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Authority
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuthorityRepository: JpaRepository<Authority, Long> {
    fun findByAuthority(authority: String): Authority?
}