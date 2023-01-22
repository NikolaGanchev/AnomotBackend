package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MediaRepository: JpaRepository<Media, Long> {
    fun getMediaByPublisher(user: User, pageable: Pageable): List<Media>

    @Modifying
    @Query("delete from Media m where m.publisher = ?1")
    fun deleteByUser(user: User)

    fun getByName(uuid: UUID): Media?
}