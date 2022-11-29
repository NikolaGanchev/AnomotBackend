package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.File
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FileRepository: JpaRepository<File, Long> {}