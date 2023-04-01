package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.security.password.ValidPassword
import com.anomot.anomotbackend.utils.Constants
import javax.validation.constraints.NotNull
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
                    @Size(min = 1, max = Constants.USERNAME_MAX_LENGTH, message = "Username is too long")
                    val username: String)