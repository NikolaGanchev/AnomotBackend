package com.anomot.anomotbackend.dto

import com.anomot.anomotbackend.utils.NotificationType

data class NotificationDto(
        val type: NotificationType,
        val read: Boolean,
        // payload based on the Notification type
        // For example, a new login will be string, the id of a SuccessfulLogin
        // a battle end will be a string, the id of a battle
        val payload: Any,
        val id: String
)
