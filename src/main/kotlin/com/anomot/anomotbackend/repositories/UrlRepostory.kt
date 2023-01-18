package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Url
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UrlRepository: JpaRepository<Url, Long> {
    fun getByInAppUrl(inAppUrl: String): Url?

    @Modifying
    @Query("delete from Url url where url.publisher = ?1")
    fun deleteByPublisher(user: User)
}