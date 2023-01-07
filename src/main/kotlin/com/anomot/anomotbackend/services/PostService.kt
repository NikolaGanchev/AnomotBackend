package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.PostRepository
import com.anomot.anomotbackend.utils.PostType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

enum class PostCreateStatus {
    OK,
    MEDIA_UNSUPPORTED,
    NSFW_FOUND;

    var media: MediaService.MediaUploadResult? = null
    var post: Post? = null
}

@Service
class PostService @Autowired constructor(
        private val postRepository: PostRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val mediaService: MediaService
) {

    fun addTextPost(text: String, user: User): Post {
        return postRepository.save(Post(user, null, text, PostType.TEXT))
    }

    fun addMediaPost(media: Media, user: User): Post {
        return postRepository.save(Post(user, media, null, PostType.MEDIA))
    }

    fun createTextPost(text: String, user: User): Post {
        return addTextPost(text, user)
    }

    fun createMediaPost(file: MultipartFile, user: User, shouldHash: Boolean): PostCreateStatus {
        val media = mediaService.uploadMedia(file, false, true, user)

        if (media?.media == null) {
            return PostCreateStatus.MEDIA_UNSUPPORTED
        }

        val nsfwStats = media.maxNsfwScan ?: media.avgNsfwScan!!


        if (!mediaService.inNsfwRequirements(nsfwStats)) {
            return PostCreateStatus.NSFW_FOUND.also {
                it.media = media
            }
        }

        val post = addMediaPost(media.media, user)

        return PostCreateStatus.OK.also {
            it.post = post
            it.media = media
        }
    }
}