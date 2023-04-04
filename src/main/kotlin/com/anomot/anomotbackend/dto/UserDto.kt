package com.anomot.anomotbackend.dto

import java.io.Serializable

data class UserDto(val username: String,
                   val id: String,
                   val avatarId: String? = null): Serializable
