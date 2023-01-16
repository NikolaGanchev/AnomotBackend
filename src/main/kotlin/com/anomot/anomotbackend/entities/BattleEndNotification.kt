package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.NotificationType
import java.io.Serializable
import javax.persistence.*

@Entity
class BattleEndNotification(
        user: User,
        @ManyToOne
        val battle: Battle
): Serializable, Notification(user, NotificationType.BATTLE_END)