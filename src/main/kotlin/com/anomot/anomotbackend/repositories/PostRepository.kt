package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostRepository: JpaRepository<Post, Long> {}