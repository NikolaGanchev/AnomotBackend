package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.entities.Follow
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.FollowRepository
import com.anomot.anomotbackend.utils.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class FollowService @Autowired constructor(
        private val followRepository: FollowRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {
    fun follow(user: User, userToFollow: User): Boolean {
        if (user.id == userToFollow.id) {
            return false
        }

        if (!followRepository.canSeeAccount(user, userToFollow)) return false

        val follow = Follow(userToFollow, user)
        followRepository.save(follow)
        return true
    }

    fun canSeeOtherUser(user: User, userToSee: User): Boolean {
        return followRepository.canSeeAccount(user, userToSee)
    }

    @Transactional
    fun unfollow(user: User, userToUnfollow: User): Boolean {
        if (user.id == userToUnfollow.id) {
            return false
        }
        val num = followRepository.delete(user, userToUnfollow)
        return num > 0
    }

    fun getFollowers(user: User, pageNumber: Int): List<UserDto> {
        val page = PageRequest.of(pageNumber, Constants.FOLLOWS_PER_PAGE)

        return followRepository.getFollowedFollowers(user, page).map {
            return@map userDetailsServiceImpl.getAsDto(it.follower)
        }
    }

    fun getFollowed(user: User, pageNumber: Int): List<UserDto> {
        val page = PageRequest.of(pageNumber, Constants.FOLLOWS_PER_PAGE, Sort.by("followerId").ascending())

        return followRepository.getFollowsByFollower(user, page).map {
            return@map userDetailsServiceImpl.getAsDto(it.followed)
        }
    }

    fun getFollowerCount(user: User): Long {
        return followRepository.countFollowsByFollowed(user)
    }

    fun getFollowedCount(user: User): Long {
        return followRepository.countFollowsByFollower(user)
    }

    fun follows(user: User, followed: User): Boolean {
        return followRepository.existsFollowByFollowerAndFollowed(user, followed)
    }
}