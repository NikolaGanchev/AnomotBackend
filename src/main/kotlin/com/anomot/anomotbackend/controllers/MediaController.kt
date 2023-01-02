package com.anomot.anomotbackend.controllers

import com.anomot.anomotbackend.services.MediaService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@RestController
@RequestMapping("/media")
class MediaController(private val mediaService: MediaService) {
    @GetMapping
    fun getMedia(@RequestParam @Min(36) @Max(36) id: String): ResponseEntity<ByteArray> {
        val media = mediaService.getMediaFromServer(id) ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        return ResponseEntity.ok()
                .contentType(media.contentType)
                .body(media.file)
    }
}