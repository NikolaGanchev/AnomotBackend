package com.anomot.anomotbackend.dto

data class UserDto(val username: String,
                   val id: String,
                   val avatarId: String? = null)
