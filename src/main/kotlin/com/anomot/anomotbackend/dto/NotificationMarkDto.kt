package com.anomot.anomotbackend.dto

import jakarta.validation.constraints.Size

data class NotificationMarkDto(
        @Size(min = 1, max = 100)
        val notificationIds: List<String>,
        val isRead: Boolean
)
