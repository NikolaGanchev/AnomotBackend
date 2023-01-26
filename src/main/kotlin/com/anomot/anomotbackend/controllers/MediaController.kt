package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.dto.MediaDto
import com.anomot.anomotbackend.dto.UrlDto
import com.anomot.anomotbackend.dto.UrlUploadDto
import com.anomot.anomotbackend.dto.UrlUploadedDto
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.security.EmailVerified
import com.anomot.anomotbackend.services.MediaService
import com.anomot.anomotbackend.services.UserDetailsServiceImpl
import com.anomot.anomotbackend.utils.Constants
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@RestController
@RequestMapping
class MediaController(
        private val mediaService: MediaService,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {

    @PostMapping("/account/url")
    @EmailVerified
    fun uploadUrl(@RequestBody @Valid urlUploadDto: UrlUploadDto, authentication: Authentication): ResponseEntity<UrlUploadedDto> {
        val user = userDetailsServiceImpl.getUserReferenceFromDetails((authentication.principal) as CustomUserDetails)

        return ResponseEntity(UrlUploadedDto(mediaService.uploadUrl(urlUploadDto.url, user)), HttpStatus.CREATED)
    }

    @GetMapping("/url/{url}")
    fun getUrl(@PathVariable(value="url") @Min(Constants.MIN_URL_LENGTH.toLong()) @Max(Constants.URL_LENGTH.toLong()) url: String): ResponseEntity<UrlDto> {
        val realUrl = mediaService.getRealUrl(url)
        return if (realUrl == null) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            ResponseEntity(UrlDto(realUrl, listOf()), HttpStatus.OK)
        }
    }

    @GetMapping("/media/{id}")
    fun getMedia(@PathVariable(value="id") @Min(36) @Max(36) id: String): ResponseEntity<ByteArray> {
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

    @PostMapping("/account/media/promote/battle/{id}")
    fun promoteToBattle(@PathVariable(value="id") @Min(36) @Max(36) id: String) {
        //TODO
        //Check if not in battle, battle queue or post
        //normal upload process
    }

    @PostMapping("/account/media/promote/post/{id}")
    fun promoteToPost(@PathVariable(value="id") @Min(36) @Max(36) id: String) {
        //TODO
        //Check if not in battle, battle queue or post
        //normal upload process
    }
}