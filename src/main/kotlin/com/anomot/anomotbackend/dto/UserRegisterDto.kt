package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.security.password.ValidPassword
import com.anomot.anomotbackend.utils.Constants
import org.jetbrains.annotations.NotNull
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class UserRegisterDto (@NotNull @NotEmpty
                    @Email(regexp ="[a-z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*", message = "Email is not valid")
                    @Size(max=254, message = "Email is too long")
                    val email: String,
                    @ValidPassword
                    val password: String,
                    @NotNull
                    @NotEmpty
                    @Size(min = 1, max = Constants.USERNAME_MAX_LENGTH, message = "Username is too long")
                    val username: String)