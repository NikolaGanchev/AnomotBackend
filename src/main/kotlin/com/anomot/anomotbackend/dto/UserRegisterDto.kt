package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.security.password.ValidPassword
import org.jetbrains.annotations.NotNull
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

data class UserRegisterDto (@NotNull @NotEmpty
                    @Email(regexp ="[a-z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*", message = "Email is not valid")
                    @Size(max=254, message = "Email is too long")
                    val email: String,
                    @ValidPassword
                    val password: String,
                    @NotNull
                    @NotEmpty
                    @Size(min = 1, max = 40, message = "Username is too long")
                    val username: String)