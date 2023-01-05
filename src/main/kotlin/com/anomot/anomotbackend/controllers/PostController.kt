package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.TextPostDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.MediaService
import com.anomot.anomotbackend.services.PostService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/account/post")
class PostController @Autowired constructor(
        private val postService: PostService,
        private val mediaService: MediaService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {
    @PostMapping("/text")
    fun uploadTextPost(@RequestBody textPostDto: TextPostDto, authentication: Authentication): ResponseEntity<String> {
        postService.addTextPost(textPostDto.text, (authentication.principal) as CustomUserDetails)
        return ResponseEntity(HttpStatus.CREATED)
    }

    @PostMapping("/media")
    fun uploadMediaPost(@RequestParam("file") file: MultipartFile, authentication: Authentication): ResponseEntity<String> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val media = mediaService.uploadMedia(file, false, true, user)

        if (media?.media == null) {
            return ResponseEntity(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        }

        if (!mediaService.inNsfwRequirements(media.maxNsfwScan!!)) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        postService.addMediaPost(media.media, (authentication.principal) as CustomUserDetails)

        return ResponseEntity(HttpStatus.CREATED)
    }
}