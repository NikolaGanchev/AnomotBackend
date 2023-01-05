package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.repositories.PostRepository
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.utils.PostType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PostService @Autowired constructor(
        private val postRepository: PostRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {

    fun addTextPost(text: String, userDetails: CustomUserDetails): Post {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails(userDetails)
        return postRepository.save(Post(user, null, text, PostType.TEXT))
    }

    fun addMediaPost(media: Media, userDetails: CustomUserDetails): Post {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails(userDetails)
        return postRepository.save(Post(user, media, null, PostType.MEDIA))
    }
}