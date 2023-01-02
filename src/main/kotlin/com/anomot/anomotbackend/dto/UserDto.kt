package com.anomot.anomotbackend.dto

data class UserDto(val email: String,
                   val username: String,
                   val id: Long,
                   val avatarId: String? = null)
