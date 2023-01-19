package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.MediaDto
import com.anomot.anomotbackend.dto.PostDto
import com.anomot.anomotbackend.dto.PostWithLikes
import com.anomot.anomotbackend.dto.UserDto
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.MediaType
import com.anomot.anomotbackend.utils.PostType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.lang.NumberFormatException
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
        private val postRepository: PostRepository,
        private val mediaService: MediaService,
        private val battleQueueRepository: BattleQueueRepository,
        private val battleRepository: BattleRepository,
        private val followService: FollowService,
        private val likeRepository: LikeRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val voteRepository: VoteRepository
) {
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
            val similar = battleRepository.getSimilarMedia(user, media.media,
                    if (media.media.mediaType == MediaType.VIDEO && media.media.duration != null) {
                        media.media.duration
                    } else 0f)
            if (similar.isNotEmpty()) {
                return PostCreateStatus.SIMILAR_FOUND.also {
                    it.media = media
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

    // Comes with a 30 elo subtraction if the post was in a battle
    // Deleting a post will make everyone who has voted for you or has had a battle against you but hasn't followed you lose access to your account
    // unless they gained it from another battle or vote
    // Followers will still have access
    @Transactional
    fun deletePost(postId: Long, user: User): Boolean {
        if (!postRepository.existsById(postId)) {
            return false
        }

        val post = postRepository.getReferenceById(postId)
        if (post.poster.id != user.id) return false

        battleQueueRepository.deletePostByIdAndUser(postId, user.id!!)
        likeRepository.deleteByPostAndPostPoster(post, user)
        voteRepository.setPostToNull(post)
        val existsBattle = battleRepository.existsByRedPostOrGoldPost(post)
        if (existsBattle) {
            battleRepository.setPostToNull(post)
            user.elo -= 30
        }

        if (post.media != null) {
            mediaService.deleteMedia(post.media!!)
        }

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

        val post = postRepository.getReferenceById(postIdLong)

        if (!canSeeUserAndPost(user, post)) return false

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

        if (!canSeeUserAndPost(user, post)) return false

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

        val post = postRepository.getReferenceById(postIdLong)

        if (!canSeeUserAndPost(user, post)) return null

        return likeRepository.getLikedByByUserAndPost(user, post, PageRequest.of(page, Constants.LIKED_BY_PAGE)).map {
            userDetailsServiceImpl.getAsDto(it)
        }
    }

    fun getPost(user: User, post: Post): PostDto? {
        if (!canSeeUserAndPost(user, post)) return null

        val postWithLikes = postRepository.getWithLikesByPostId(user, post) ?: return null
        val p = postWithLikes.post ?: return null

        return PostDto(p.type,
                p.text,
                if (p.media != null) MediaDto(p.media!!.mediaType, p.media!!.name.toString()) else null,
                userDetailsServiceImpl.getAsDto(p.poster),
                postWithLikes.likes,
                postWithLikes.hasUserLiked,
                p.creationDate,
                p.id.toString())
    }
}