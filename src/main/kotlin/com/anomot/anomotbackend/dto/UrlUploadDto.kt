package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.security.text.ValidUrl
import jakarta.validation.constraints.Max


data class UrlUploadDto(
        @Max(2000)
        @ValidUrl
        val url: String
)
