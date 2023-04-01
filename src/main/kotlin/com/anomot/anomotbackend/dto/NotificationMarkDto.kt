package com.anomot.anomotbackend.dto

import javax.validation.constraints.Size

data class NotificationMarkDto(
        @Size(min = 1, max = 100)
        val notificationIds: List<String>,
        val isRead: Boolean
)
