package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import org.jetbrains.annotations.NotNull
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

data class ChatCreationDto(
        @NotNull
        @NotEmpty
        @Size(min = 1, max = Constants.MAX_CHAT_TITLE_LENGTH, message = "Title has to be between 1 and ${Constants.MAX_CHAT_TITLE_LENGTH} characters")
        val title: String,
        @Size(min = 1, max = Constants.MAX_POST_LENGTH, message = "Description has to be between 1 and ${Constants.MAX_POST_LENGTH} characters")
        val description: String?,
        @Size(min = 1, max = Constants.MAX_COMMENT_SIZE, message = "Info has to be between 1 and ${Constants.MAX_POST_LENGTH} characters")
        val info: String?,
        @Size(min = 1, max = Constants.PASSWORD_MAX_SIZE, message = "Password has to be between 1 and ${Constants.PASSWORD_MAX_SIZE} characters")
        val password: String?,
        @NotNull
        @NotEmpty
        @Size(min = 1, max = Constants.USERNAME_MAX_LENGTH, message = "Username has to be between 1 and ${Constants.USERNAME_MAX_LENGTH} characters")
        val chatUsername: String,
)
