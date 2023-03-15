package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.NotificationType
import java.io.Serializable
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne

@Entity
class NewLoginNotification(
        user: User,
        @ManyToOne
        val successfulLogin: SuccessfulLogin
): Serializable, Notification(user, NotificationType.NEW_LOGIN)