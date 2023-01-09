package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.MediaDto
import com.anomot.anomotbackend.dto.PostDto
import com.anomot.anomotbackend.dto.TextPostDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.EmailVerified
import com.anomot.anomotbackend.services.FollowService
import com.anomot.anomotbackend.services.PostCreateStatus
import com.anomot.anomotbackend.services.PostService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping
class PostController @Autowired constructor(
        private val postService: PostService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val followService: FollowService
) {
    @PostMapping("/account/post/text")
    @EmailVerified
    fun uploadTextPost(@RequestBody textPostDto: TextPostDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        postService.addTextPost(textPostDto.text, user)
        return ResponseEntity(HttpStatus.CREATED)
    }

    @PostMapping("/account/post/media")
    @EmailVerified
    fun uploadMediaPost(@RequestParam("file") file: MultipartFile, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return when (postService.createMediaPost(file, user, false)) {
            PostCreateStatus.OK -> ResponseEntity(HttpStatus.CREATED)
            PostCreateStatus.MEDIA_UNSUPPORTED -> ResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            PostCreateStatus.NSFW_FOUND -> ResponseEntity(HttpStatus.BAD_REQUEST)
            PostCreateStatus.SIMILAR_FOUND -> ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/account/posts")
    fun getSelfPosts(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<PostDto>> {
        val posts = postService.getPostsForUser(
                userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                page).map {
                    PostDto(it.type,
                            it.text,
                            if (it.media != null) MediaDto(it.media!!.mediaType, it.media!!.name.toString()) else null,
                            userDetailsServiceImpl.getAsDto(it.poster!!),
                            0,
                            it.creationDate,
                            it.id.toString())
        }

        return ResponseEntity(posts, HttpStatus.OK)
    }

    @DeleteMapping("/account/post")
    fun deleteSelfPost(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        return try {
            ResponseEntity(if (postService.deletePost(id.toLong(), user)) HttpStatus.OK else HttpStatus.NOT_FOUND)
        } catch (numberFormatException: NumberFormatException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/posts")
    @EmailVerified
    fun getOtherUserPosts(@RequestParam("userId") userId: String,
                      @RequestParam("page") page: Int,
                      authentication: Authentication): ResponseEntity<List<PostDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val otherUser = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(userId) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        if (!followService.follows(user, otherUser)) return ResponseEntity(HttpStatus.FORBIDDEN)

        val posts = postService.getPostsForUser(
                otherUser,
                page).map {
            PostDto(it.type,
                    it.text,
                    if (it.media != null) MediaDto(it.media!!.mediaType, it.media!!.name.toString()) else null,
                    userDetailsServiceImpl.getAsDto(it.poster!!),
                    0,
                    it.creationDate,
                    it.id.toString())
        }

        return ResponseEntity(posts, HttpStatus.OK)
    }

    @GetMapping("/feed")
    fun getFeed(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<PostDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val posts = postService.getFeed(
                user,
                page).map {
            if (it.poster == null) return@map null

            PostDto(it.type,
                    it.text,
                    if (it.media != null) MediaDto(it.media!!.mediaType, it.media!!.name.toString()) else null,
                    userDetailsServiceImpl.getAsDto(it.poster!!),
                    0,
                    it.creationDate,
                    it.id.toString())
        }.filterNotNull()

        return ResponseEntity(posts, HttpStatus.OK)
    }
}