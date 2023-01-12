package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.security.text.TextPostSize
import com.anomot.anomotbackend.utils.Constants
import javax.validation.constraints.Size

data class TextPostDto(
        @TextPostSize(absoluteMin = Constants.ABSOLUTE_POST_TEXT_MIN_SIZE_LIMIT,
                        absoluteMax = Constants.ABSOLUTE_POST_TEXT_MAX_SIZE_LIMIT,
                        min = Constants.MIN_POST_LENGTH,
                        max = Constants.MAX_POST_LENGTH)
        val text: String
)
