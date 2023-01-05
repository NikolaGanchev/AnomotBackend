package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import javax.validation.constraints.Size

data class TextPostDto(
        @Size(min = 1)
        @Size(max = Constants.MAX_POST_LENGTH)
        val text: String
)
