package com.anomot.anomotbackend.dto

data class NsfwFoundDto(
        private val appealJwt: String,
        private val mediaDto: MediaDto
)