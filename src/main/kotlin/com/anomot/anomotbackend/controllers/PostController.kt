package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.EmailVerified
import com.anomot.anomotbackend.services.*
import com.anomot.anomotbackend.utils.AppealObjective
import com.anomot.anomotbackend.utils.AppealReason
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
        private val followService: FollowService,
        private val userModerationService: UserModerationService
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
    fun uploadMediaPost(@RequestParam("file") file: MultipartFile, authentication: Authentication): ResponseEntity<NsfwFoundDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return when (val result = postService.createMediaPost(file, user, false)) {
            PostCreateStatus.OK -> ResponseEntity(HttpStatus.CREATED)
            PostCreateStatus.MEDIA_UNSUPPORTED -> ResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            PostCreateStatus.NSFW_FOUND -> {
                ResponseEntity(
                        NsfwFoundDto(
                                userModerationService.generateAppealJwt(user,
                                        result.media!!.media!!.toString(),
                                        AppealReason.NSFW_FOUND,
                                        AppealObjective.POST),
                                MediaDto(result.media!!.media!!.mediaType,
                                        result.media!!.media!!.name.toString())),
                        HttpStatus.NOT_ACCEPTABLE) }
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
    fun getOtherUserPosts(@RequestParam("id") userId: String,
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
    fun like(@RequestParam("id") postId: String, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = postService.like(user, postId)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/unlike")
    @EmailVerified
    fun unlike(@RequestParam("id") postId: String, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = postService.unlike(user, postId)
        return ResponseEntity(if (result) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/likes")
    fun getLikes(@RequestParam("page") page: Int, @RequestParam("id") postId: String, authentication: Authentication): ResponseEntity<List<UserDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = postService.getLikedBy(user, postId, page)
        return ResponseEntity(result, if (result != null) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/post/report")
    @EmailVerified
    fun reportPost(@RequestBody @Valid postReportDto: PostReportDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = postService.report(postReportDto, user)

        return ResponseEntity(if (result) HttpStatus.CREATED else HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/post/report")
    fun getPostReport(@RequestParam("id") postId: String, authentication: Authentication): ResponseEntity<ReportDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        val result = postService.getReport(user, postId)

        return if (result != null) {
            ResponseEntity(result, HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }
}