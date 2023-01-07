package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Follow
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.utils.Constants
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FollowRepository: JpaRepository<Follow, Long> {

    // Get the followers of a user
    fun getFollowsByFollowed(followed: User, pageable: Pageable =
            PageRequest.of(0, Constants.FOLLOWS_PER_PAGE, Sort.by("followerId").ascending())): List<Follow>

    // Get the users a user follows
    fun getFollowsByFollower(follower: User, pageable: Pageable =
            PageRequest.of(0, Constants.FOLLOWS_PER_PAGE, Sort.by("followedId").ascending())): List<Follow>

    // Get number of followers
    fun countFollowsByFollowed(followed: User): Long

    // Get number of followed users
    fun countFollowsByFollower(follower: User): Long

    // Get followers you also follow back
    // These should be the only ones visible to the user
    @Query("with followers as (select * from follow where followed_id = ?1), " +
            "followed as (select * from follow where follower_id = ?1) " +
            "select followers.id, followers.followed_id, followers.follower_id from followers inner join followed on followers.follower_id = followed.followed_id ",
            nativeQuery = true)
    fun getFollowedFollowers(user: User, pageable: Pageable =
        PageRequest.of(0, Constants.FOLLOWS_PER_PAGE)): List<Follow>

    fun existsFollowByFollowerAndFollowed(follower: User, followed: User): Boolean

    @Modifying
    @Query("delete from Follow f where f.followed = ?2 and f.follower = ?1")
    fun delete(user: User, userToUnfollow: User)
}