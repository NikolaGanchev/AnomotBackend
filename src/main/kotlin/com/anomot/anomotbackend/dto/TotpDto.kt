package com.anomot.anomotbackend.dto

data class TotpDto(
        val secret: String,
        val uri: String
)