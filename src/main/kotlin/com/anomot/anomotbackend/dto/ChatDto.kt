package com.anomot.anomotbackend.dto

import java.util.*

data class ChatDto(
        var title: String,
        var description: String?,
        var info: String?,
        val creationDate: Date = Date(),
)
