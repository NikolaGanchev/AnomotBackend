package com.anomot.anomotbackend.dto

data class UserDto(val email: String,
                   val username: String,
                   val roles: List<String>,
                   val isMfaActive: Boolean,
                   val mfaMethods: List<String>? = null,)