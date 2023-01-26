package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Ban
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
@Repository
interface BanRepository: JpaRepository<Ban, Long> {

    @Query("from Ban b where b.user = ?1 and b.until > current_timestamp")
    fun getActive(user: User, pageable: Pageable): List<Ban>

    fun getAllByUser(user: User, pageable: Pageable): List<Ban>

    fun deleteByUser(user: User)

    @Query("update Ban b set b.bannedBy = NULL where b.bannedBy = ?1")
    @Modifying
    fun setNullByBannedBy(bannedBy: User)
}