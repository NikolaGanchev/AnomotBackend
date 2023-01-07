package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.PostDto
import com.anomot.anomotbackend.dto.TextPostDto
import com.anomot.anomotbackend.security.CustomUserDetails
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
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {
    @PostMapping("/account/post/text")
    fun uploadTextPost(@RequestBody textPostDto: TextPostDto, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        postService.addTextPost(textPostDto.text, user)
        return ResponseEntity(HttpStatus.CREATED)
    }

    @PostMapping("/account/post/media")
    fun uploadMediaPost(@RequestParam("file") file: MultipartFile, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return when (postService.createMediaPost(file, user, false)) {
            PostCreateStatus.OK -> ResponseEntity(HttpStatus.CREATED)
            PostCreateStatus.MEDIA_UNSUPPORTED -> ResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            PostCreateStatus.NSFW_FOUND -> ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/account/posts")
    fun getSelfPosts(authentication: Authentication): ResponseEntity<List<PostDto>> {
        //TODO
        return ResponseEntity(HttpStatus.OK)
    }

    @GetMapping("/account/posts")
    fun getSelfPost(@RequestParam("id") id: String, authentication: Authentication): ResponseEntity<PostDto> {
        //TODO
        return ResponseEntity(HttpStatus.OK)
    }

    @DeleteMapping("/account/post")
    fun deleteSelfPost(@RequestParam("id") id: String, authentication: Authentication) {
        //TODO
    }

    @GetMapping("posts")
    fun getOtherPosts(@RequestParam("userId") userId: String, authentication: Authentication): ResponseEntity<List<PostDto>> {
        //TODO
        return ResponseEntity(HttpStatus.OK)
    }

    @GetMapping("/account/posts")
    fun getOtherPost(@RequestParam("userId") userId: String,
                     @RequestParam("id") id: String,
                     authentication: Authentication): ResponseEntity<PostDto> {
        //TODO
        return ResponseEntity(HttpStatus.OK)
    }
}