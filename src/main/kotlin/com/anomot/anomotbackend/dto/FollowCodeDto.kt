package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.Constants
import javax.validation.constraints.Size

data class FollowCodeDto(
        @Size(min = Constants.FOLLOW_CODE_LENGTH, max = Constants.FOLLOW_CODE_LENGTH)
        val code: String
)
