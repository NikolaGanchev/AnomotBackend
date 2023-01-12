package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Url
import org.springframework.data.jpa.repository.JpaRepository

interface UrlRepository: JpaRepository<Url, Long> {
    fun getByInAppUrl(inAppUrl: String): Url?
}