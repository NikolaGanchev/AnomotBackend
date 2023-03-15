package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import org.jetbrains.annotations.NotNull
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class LoginDto(
    @NotNull
    @NotEmpty
    @Email(regexp ="[a-z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*", message = "Email is not valid")
    @Size(max=254, message = "Email is too long")
    val email: String?,
    @NotNull
    @NotEmpty
    @Size(min = Constants.PASSWORD_MIN_SIZE, max = Constants.PASSWORD_MAX_SIZE)
    val password: String?,
    val rememberMe: Boolean?)