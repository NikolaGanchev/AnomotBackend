package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import org.jetbrains.annotations.NotNull
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class UsernameChangeDto(
        @NotNull
        @NotEmpty
        @Size(min = 1, max = Constants.USERNAME_MAX_LENGTH, message = "Username is too long")
        val username: String
)