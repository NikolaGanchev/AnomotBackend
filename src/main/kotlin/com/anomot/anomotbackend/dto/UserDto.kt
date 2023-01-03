package com.anomot.anomotbackend.dto

data class UserDto(val username: String,
                   val id: Long,
                   val avatarId: String? = null)
