package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.repositories.BattleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FindFinishedBattlesService@Autowired constructor(
        val battleRepository: BattleRepository,
        val battleService: BattleService,
        val notificationService: NotificationService
) {

    @Scheduled(fixedRate = 5 * 1000)
    @Transactional
    fun findFinishedBattles() {
        val battles = battleRepository.getUnprocessedFinishedBattlesAndUpdate()
        battles.parallelStream().forEach {
            try {
                battleService.finish(it)
                if (it.goldPost != null) {
                    notificationService.sendBattleEndNotification(it.goldPost!!.poster, it)
                    notificationService.sendBattleEndNotificationToVotersAndPost(it, it.goldPost!!)
                }
                if (it.redPost != null) {
                    notificationService.sendBattleEndNotification(it.redPost!!.poster, it)
                    notificationService.sendBattleEndNotificationToVotersAndPost(it, it.redPost!!)
                }
            } catch (_: Exception) {}
        }
    }
}