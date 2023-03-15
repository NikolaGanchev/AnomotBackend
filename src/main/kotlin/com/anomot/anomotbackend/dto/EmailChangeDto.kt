package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.security.password.ValidPassword
import org.jetbrains.annotations.NotNull
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class EmailChangeDto(
        @NotNull @NotEmpty
        @Email(regexp ="[a-z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*", message = "Email is not valid")
        @Size(max=254, message = "Email is too long")
        val newEmail: String,
        @ValidPassword
        val password: String
)