package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.File
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FileRepository: JpaRepository<File, Long> {

    @Modifying
    @Query("delete from File f where f.uploader = ?1")
    fun deleteByUser(user: User)
    fun getFilesByUploader(user: User, pageable: Pageable): List<File>
}