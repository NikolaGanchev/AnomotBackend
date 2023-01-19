package com.anomot.anomotbackend.dto

import org.jetbrains.annotations.NotNull
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

data class UsernameChangeDto(
        @NotNull
        @NotEmpty
        @Size(min = 1, max = 40, message = "Username is too long")
        val username: String
)