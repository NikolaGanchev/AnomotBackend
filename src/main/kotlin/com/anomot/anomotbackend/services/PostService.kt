package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.PostWithLikes
import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.entities.Like
import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.BattleQueueRepository
import com.anomot.anomotbackend.repositories.BattleRepository
import com.anomot.anomotbackend.repositories.LikeRepository
import com.anomot.anomotbackend.repositories.PostRepository
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.PostType
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.lang.NumberFormatException
import java.util.*
import javax.crypto.SecretKey
import javax.transaction.Transactional

enum class PostCreateStatus {
    OK,
    MEDIA_UNSUPPORTED,
    NSFW_FOUND,
    SIMILAR_FOUND;

    var media: MediaService.MediaUploadResult? = null
    var post: Post? = null
    var similar: List<Post>? = null
}

@Service
class PostService @Autowired constructor(
        @Value("\${vote.jwt.private-key}") private val secretKeyString: String,
        private val postRepository: PostRepository,
        private val mediaService: MediaService,
        private val battleQueueRepository: BattleQueueRepository,
        private val battleRepository: BattleRepository,
        private val followService: FollowService,
        private val likeRepository: LikeRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyString))

    private fun addTextPost(text: String, user: User): Post {
        return postRepository.save(Post(user, null, text, PostType.TEXT))
    }

    private fun addMediaPost(media: Media, user: User): Post {
        return postRepository.save(Post(user, media, null, PostType.MEDIA))
    }

    fun createTextPost(text: String, user: User, checkSimilar: Boolean): PostCreateStatus {
        if (checkSimilar) {
            val same = battleRepository.getWithSameText(user, text)
            if (same.isNotEmpty()) {
                return PostCreateStatus.SIMILAR_FOUND.also {
                    it.similar = same
                }
            }
        }

        return PostCreateStatus.OK.also {
            it.post = addTextPost(text, user)
        }
    }

    fun createMediaPost(file: MultipartFile, user: User, shouldHash: Boolean): PostCreateStatus {
        val media = mediaService.uploadMedia(file, shouldHash, true, user)

        if (media?.media == null) {
            return PostCreateStatus.MEDIA_UNSUPPORTED
        }

        val nsfwStats = media.maxNsfwScan ?: media.avgNsfwScan!!

        if (!mediaService.inNsfwRequirements(nsfwStats)) {
            return PostCreateStatus.NSFW_FOUND.also {
                it.media = media
            }
        }

        if (shouldHash) {
            val similar = battleRepository.getSimilarMedia(user, media.media)
            if (similar.isNotEmpty()) {
                return PostCreateStatus.SIMILAR_FOUND.also {
                    it.similar = similar
                }
            }
        }

        val post = addMediaPost(media.media, user)

        return PostCreateStatus.OK.also {
            it.post = post
            it.media = media
        }
    }

    fun getPostsForUser(user: User, fromUser: User, page: Int): List<PostWithLikes> {
        return if (user.id == fromUser.id) {
            postRepository.findAllByPosterSelf(user, PageRequest.of(page, Constants.POST_PAGE, Sort.by("creationDate").descending()))
        }
        else {
            postRepository.findAllByPosterOther(user, fromUser, PageRequest.of(page, Constants.POST_PAGE, Sort.by("creationDate").descending()))
        }
    }

    fun getPostReferenceFromIdUnsafe(id: String): Post? {
        return try {
            if (postRepository.existsById(id.toLong())) {
                postRepository.getReferenceById(id.toLong())
            } else null
        } catch(numberFormatException: NumberFormatException) {
            null
        }
    }

    fun canSeeUserAndPost(user: User, post: Post): Boolean {
        return followService.canSeeOtherUser(user, post.poster) && postRepository.canSeePost(user, post.poster)
    }

    @Transactional
    fun deletePost(postId: Long, user: User): Boolean {
        if (!postRepository.existsById(postId)) {
            return false
        }

        val post = postRepository.getReferenceById(postId)
        if (post.poster.id != user.id) return false

        battleQueueRepository.deletePostByIdAndUser(postId, user.id!!)
        likeRepository.deleteByPostAndPostPoster(post, user)
        val num = postRepository.deleteByIdAndPoster(postId, user)

        return num != (0).toLong()
    }

    fun getFeed(user: User, page: Int): List<PostWithLikes> {
        return postRepository.getFeed(user, PageRequest.of(page, Constants.FEED_PAGE, Sort.by("creationDate").descending()))
    }

    fun like(user: User, postId: String): Boolean {
        val postIdLong = try {
            postId.toLong()
        } catch (exception: NumberFormatException) {
            return false
        }

        if (!postRepository.existsById(postIdLong)) return false

        val poster = postRepository.findPosterById(postIdLong)

        if (!followService.follows(user, poster)) return false

        val post = postRepository.getReferenceById(postIdLong)

        if (likeRepository.existsByLikedByAndPost(user, post)) return false

        likeRepository.save(Like(post, user))

        return true
    }

    @Transactional
    fun unlike(user: User, postId: String): Boolean {
        val postIdLong = try {
            postId.toLong()
        } catch (exception: NumberFormatException) {
            return false
        }

        if (!postRepository.existsById(postIdLong)) return false

        val post = postRepository.getReferenceById(postIdLong)

        val deleted = likeRepository.deleteByLikedByAndPost(user, post)

        return deleted > 0
    }

    fun getLikedBy(user: User, postId: String, page: Int): List<UserDto>? {
        val postIdLong = try {
            postId.toLong()
        } catch (exception: NumberFormatException) {
            return null
        }

        if (!postRepository.existsById(postIdLong)) return null

        val poster = postRepository.findPosterById(postIdLong)

        if (!followService.follows(user, poster)) return null

        val post = postRepository.getReferenceById(postIdLong)

        return likeRepository.getLikedByByUserAndPost(user, post, PageRequest.of(page, Constants.LIKED_BY_PAGE)).map {
            userDetailsServiceImpl.getAsDto(it)
        }
    }
}