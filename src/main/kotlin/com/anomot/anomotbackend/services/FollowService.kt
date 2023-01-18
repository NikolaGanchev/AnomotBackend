package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.FollowCodeDto
import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.entities.Follow
import com.anomot.anomotbackend.entities.FollowCode
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.FollowCodeRepository
import com.anomot.anomotbackend.repositories.FollowRepository
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.SecureRandomStringGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class FollowService @Autowired constructor(
        private val followRepository: FollowRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val followCodeRepository: FollowCodeRepository
) {
    private val codeGenerator = SecureRandomStringGenerator(SecureRandomStringGenerator.ALPHANUMERIC)

    fun follow(user: User, userToFollow: User): Boolean {
        if (user.id == userToFollow.id) {
            return false
        }

        if (!followRepository.canSeeAccount(user, userToFollow)) return false
        if (follows(user, userToFollow)) return false

        val follow = Follow(userToFollow, user)
        followRepository.save(follow)
        return true
    }

    fun canSeeOtherUser(user: User, userToSee: User): Boolean {
        if (user.id == userToSee.id) return true
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

    fun getFollowCode(user: User): FollowCodeDto {
        val followCode = followCodeRepository.findByUser(user)

        if (followCode == null) {
            // There is a negligible chance of code collisions
            val code = codeGenerator.generate(Constants.FOLLOW_CODE_LENGTH)
            val result = followCodeRepository.save(FollowCode(user, code, Date()))
            return FollowCodeDto(result.code)
        }

        return FollowCodeDto(followCode.code)
    }

    @Transactional
    fun useFollowCode(user: User, code: String): UserDto? {
        val followCode = followCodeRepository.findByCode(code) ?: return null

        val userToFollow = followCode.user
        if (userToFollow.id == user.id) return null
        if (follows(user, userToFollow)) return null

        val follow = Follow(userToFollow, user)
        followCode.code = codeGenerator.generate(Constants.FOLLOW_CODE_LENGTH)
        followCode.creationDate = Date()
        followRepository.save(follow)

        return userDetailsServiceImpl.getAsDto(userToFollow)
    }
}