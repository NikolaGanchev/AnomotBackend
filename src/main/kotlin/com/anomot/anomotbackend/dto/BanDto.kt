package com.anomot.anomotbackend.dto

import java.util.*

data class BanDto(
    val creationDate: Date,
    val until: Date,
    val bannedBy: UserDto?,
    val reason: String
)
