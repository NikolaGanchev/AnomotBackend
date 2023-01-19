package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.MediaDto
import com.anomot.anomotbackend.dto.PostDto
import com.anomot.anomotbackend.dto.TextPostDto
import com.anomot.anomotbackend.dto.UserDto
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
import javax.validation.Valid

@RestController
@RequestMapping
class PostController @Autowired constructor(
        private val postService: PostService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val followService: FollowService
) {
    @PostMapping("/account/post/text")
    @EmailVerified
    fun uploadTextPost(@RequestBody @Valid textPostDto: TextPostDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        postService.createTextPost(textPostDto.text, user, false)
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
                userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                page).map {
                    val post = it.post
                    PostDto(post!!.type,
                            post.text,
                            if (post.media != null) MediaDto(post.media!!.mediaType, post.media!!.name.toString()) else null,
                            userDetailsServiceImpl.getAsDto(post.poster),
                            it.likes,
                            it.hasUserLiked,
                            post.creationDate,
                            post.id.toString())
        }

        return ResponseEntity(posts, HttpStatus.OK)
    }

    @DeleteMapping("/account/post")
    @EmailVerified
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

        if (!followService.canSeeOtherUser(user, otherUser)) return ResponseEntity(HttpStatus.BAD_REQUEST)

        val posts = postService.getPostsForUser(
                otherUser,
                userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails),
                page).map {
            val post = it.post
            PostDto(post!!.type,
                    post.text,
                    if (post.media != null) MediaDto(post.media!!.mediaType, post.media!!.name.toString()) else null,
                    userDetailsServiceImpl.getAsDto(post.poster),
                    it.likes,
                    it.hasUserLiked,
                    post.creationDate,
                    post.id.toString())
        }

        return ResponseEntity(posts, HttpStatus.OK)
    }

    @GetMapping("/post")
    fun getPost(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<PostDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val post = postService.getPostReferenceFromIdUnsafe(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)

        val postDto = postService.getPost(user, post)

        return if (postDto != null) {
            ResponseEntity(postDto, HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }

    }

    @GetMapping("/feed")
    fun getFeed(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<PostDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val posts = postService.getFeed(
                user,
                page).map {
            val post = it.post ?: return@map null

            PostDto(post.type,
                    post.text,
                    if (post.media != null) MediaDto(post.media!!.mediaType, post.media!!.name.toString()) else null,
                    userDetailsServiceImpl.getAsDto(post.poster),
                    it.likes,
                    it.hasUserLiked,
                    post.creationDate,
                    post.id.toString())
        }.filterNotNull()

        return ResponseEntity(posts, HttpStatus.OK)
    }

    @PostMapping("/like")
    @EmailVerified
    fun like(@RequestParam("postId") postId: String, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = postService.like(user, postId)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/unlike")
    @EmailVerified
    fun unlike(@RequestParam("postId") postId: String, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = postService.unlike(user, postId)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/likes")
    fun getLikes(@RequestParam("page") page: Int, @RequestParam("postId") postId: String, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = postService.getLikedBy(user, postId, page)
        return ResponseEntity(result, if (result != null) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }
}