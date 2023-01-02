package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Follow
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FollowRepository: JpaRepository<Follow, Long> {

    // Get the followers of a user
    fun getFollowsByFollowed(followed: User): List<Follow>

    // Get the users a user follows
    fun getFollowsByFollower(follower: User): List<Follow>

    // Get number of followers
    fun countFollowsByFollowed(followed: User): Long

    // Get number of followed users
    fun countFollowsByFollower(follower: User): Long
}