package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.BattleQueueRepository
import com.anomot.anomotbackend.repositories.BattleRepository
import com.anomot.anomotbackend.repositories.PostRepository
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.PostType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
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
        private val battleRepository: BattleRepository
) {

    fun addTextPost(text: String, user: User): Post {
        return postRepository.save(Post(user, null, text, PostType.TEXT))
    }

    fun addMediaPost(media: Media, user: User): Post {
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

    fun getPostsForUser(user: User, page: Int): List<Post> {
        return postRepository.findAllByPoster(user, PageRequest.of(page, Constants.POST_PAGE, Sort.by("creationDate").descending()))
    }

    @Transactional
    fun deletePost(postId: Long, user: User): Boolean {
        if (!postRepository.existsById(postId)) {
            return false
        }

        battleQueueRepository.deletePostByIdAndUser(postId, user.id!!)
        val num = postRepository.deleteByIdAndPoster(postId, user)

        return num != (0).toLong()
    }

    fun getFeed(user: User, page: Int): List<Post> {
        return postRepository.getFeed(user.id!!, PageRequest.of(page, Constants.FEED_PAGE, Sort.by("creation_date").descending()))
    }
}