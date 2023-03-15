package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.NotificationType
import java.io.Serializable
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne

@Entity
class PromotionNotification(
        user: User,
        @ManyToOne
        val appeal: Appeal
): Serializable, Notification(user, NotificationType.PROMOTION)