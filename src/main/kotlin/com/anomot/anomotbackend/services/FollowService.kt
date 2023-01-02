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
        private val followRepository: FollowRepository
) {
    fun follow(user: User, userToFollow: User): Boolean {
        val follow = Follow(userToFollow, user)
        followRepository.save(follow)
        return true
    }

    @Transactional
    fun unfollow(user: User, userToUnfollow: User): Boolean {
        followRepository.delete(user, userToUnfollow)
        return true
    }

    fun getFollowers(user: User, pageNumber: Int): List<UserDto> {
        val page = PageRequest.of(pageNumber, Constants.FOLLOWS_PER_PAGE, Sort.by("followerId").ascending())

        return followRepository.getFollowsByFollowed(user, page).map {
            return@map it.follower.getAsDto()
        }
    }

    fun getFollowed(user: User, pageNumber: Int): List<UserDto> {
        val page = PageRequest.of(pageNumber, Constants.FOLLOWS_PER_PAGE, Sort.by("followerId").ascending())

        return followRepository.getFollowsByFollower(user, page).map {
            return@map it.followed.getAsDto()
        }
    }

    fun getFollowerCount(user: User): Long {
        return followRepository.countFollowsByFollowed(user)
    }

    fun getFollowedCount(user: User): Long {
        return followRepository.countFollowsByFollower(user)
    }
}