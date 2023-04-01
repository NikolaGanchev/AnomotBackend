package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import javax.validation.constraints.NotNull
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

data class UsernameChangeDto(
        @NotNull
        @NotEmpty
        @Size(min = 1, max = Constants.USERNAME_MAX_LENGTH, message = "Username is too long")
        val username: String
)