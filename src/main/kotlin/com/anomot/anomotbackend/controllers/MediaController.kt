package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.MediaDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.services.MediaService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@RestController
@RequestMapping
class MediaController(
        private val mediaService: MediaService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {
    @GetMapping("/media")
    fun getMedia(@RequestParam @Min(36) @Max(36) id: String): ResponseEntity<ByteArray> {
        val media = mediaService.getMediaFromServer(id) ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        return ResponseEntity.ok()
                .contentType(media.contentType)
                .body(media.file)
    }

    @GetMapping("/account/media")
    fun getMediaOfUser(@RequestParam("page") page: Int, authentication: Authentication): ResponseEntity<List<MediaDto>> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)
        val media = mediaService.getMediaByUser(user, page).map {
            return@map MediaDto(it.mediaType, it.name.toString())
        }
        return ResponseEntity(media, HttpStatus.OK)
    }
}