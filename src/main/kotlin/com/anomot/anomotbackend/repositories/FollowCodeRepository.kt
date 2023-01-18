package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.FollowCode
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FollowCodeRepository: JpaRepository<FollowCode, Long> {
    fun findByUser(user: User): FollowCode?

    fun findByCode(code: String): FollowCode?

    @Modifying
    @Query("delete from FollowCode f where f.user = ?1")
    fun deleteByUser(user: User)
}
