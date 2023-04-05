package com.anomot.anomotbackend.dto

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

data class ChatMemberUnbanDto(
    @NotNull
    @NotEmpty
    val chatId: String,
    @NotNull
    @NotEmpty
    val chatMemberToUnbanId: String,
    @NotNull
    @NotEmpty
    val banToRemoveId: String,
)
