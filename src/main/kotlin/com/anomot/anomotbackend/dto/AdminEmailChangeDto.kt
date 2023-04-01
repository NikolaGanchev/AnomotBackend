package com.anomot.anomotbackend.dto

import org.jetbrains.annotations.NotNull
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

data class AdminEmailChangeDto(
        @NotNull @NotEmpty
        @Email(regexp ="[a-z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*", message = "Email is not valid")
        @Size(max=254, message = "Email is too long")
        val newEmail: String
)
