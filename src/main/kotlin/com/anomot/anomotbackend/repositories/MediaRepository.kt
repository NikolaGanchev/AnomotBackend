package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Media
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MediaRepository: JpaRepository<Media, Long> {}