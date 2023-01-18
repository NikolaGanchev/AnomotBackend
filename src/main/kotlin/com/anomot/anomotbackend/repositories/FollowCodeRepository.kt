package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.FollowCode
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FollowCodeRepository: JpaRepository<FollowCode, Long> {
    fun findByUser(user: User): FollowCode?

    fun findByCode(code: String): FollowCode?
}
