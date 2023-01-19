package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.NotificationDto
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.NotificationRepository
import com.anomot.anomotbackend.repositories.VoteRepository
import com.anomot.anomotbackend.utils.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class NotificationService @Autowired constructor(
        private val notificationRepository: NotificationRepository,
        private val voteRepository: VoteRepository
) {
    fun sendBattleEndNotification(user: User, battle: Battle) {
        // TODO push notifications
        notificationRepository.save(BattleEndNotification(user, battle))
    }

    fun sendBattleEndNotificationToVotersAndPost(battle: Battle, post: Post) {
        val notifications = mutableListOf<BattleEndNotification>()
        val votes = voteRepository.findAllByBattleAndPost(battle, post)

        votes.forEach {
            notifications.add(BattleEndNotification(it.voter, battle))
        }

        notificationRepository.saveAll(notifications)
    }

    fun sendNewLoginNotification(user: User, successfulLogin: SuccessfulLogin) {
        // TODO push notifications
        notificationRepository.save(NewLoginNotification(user, successfulLogin))
    }

    fun sendBattleBeginNotification(user: User, battle: Battle) {
        // TODO push notifications
        notificationRepository.save(BattleBeginNotification(user, battle))
    }

    fun getNotifications(user: User, page: Int): List<NotificationDto> {
        return notificationRepository.findAllByUser(user, PageRequest.of(page, Constants.NOTIFICATION_PAGE, Sort.by("creationDate").descending())).map {
            val payload: Any = when(it) {
                is BattleEndNotification -> it.battle.id.toString()
                is NewLoginNotification -> it.successfulLogin.id.toString()
                else -> {}
            }
            NotificationDto(it.type, it.isRead, payload, it.id.toString())
        }
    }

    @Transactional
    fun toggleReadNotification(user: User, notificationId: String, isRead: Boolean): Boolean {
        val idLong = try {
            notificationId.toLong()
        } catch (e: NumberFormatException) {
            return false
        }

        val result = notificationRepository.setReadByUserAndId(user, idLong, isRead)

        return result > 0
    }

    @Transactional
    fun toggleReadNotifications(user: User, notificationIds: List<String>, isRead: Boolean): Boolean {
        val notificationIdsLong = notificationIds.map {
            try {
                return@map it.toLong()
            } catch (e: NumberFormatException) {
                return false
            }
        }

        val result = notificationRepository.setReadByUserAndIds(user, notificationIdsLong, isRead)

        return result > 0
    }
}