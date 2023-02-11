package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.AnomotBackendApplication
import com.anomot.anomotbackend.repositories.BattleQueueRepository
import com.anomot.anomotbackend.repositories.BattleRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@ConditionalOnProperty(
        value = ["scaling.is.main"], havingValue = "true"
)
@Component
class BattleScheduledService@Autowired constructor(
        val battleRepository: BattleRepository,
        val battleService: BattleService,
        val notificationService: NotificationService,
        val battleQueuePostRepository: BattleQueueRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(AnomotBackendApplication::class.java)
    @Scheduled(fixedRate = 5 * 1000)
    @Transactional
    fun findFinishedBattles() {
        val battles = battleRepository.getUnprocessedFinishedBattlesAndUpdate()
        battles.parallelStream().forEach {
            try {
                battleService.finish(it)
                if (it.goldPost != null) {
                    notificationService.sendBattleEndNotification(it.goldPost!!.poster, it)
                }
                if (it.redPost != null) {
                    notificationService.sendBattleEndNotification(it.redPost!!.poster, it)
                }
            } catch (_: Exception) {}
        }
    }

    //This works, however it can be slow if, for example, you try to matchmake 20000 posts
    @Scheduled(fixedDelay = 20 * 1000)
    @Transactional
    fun findBattles() {
        val posts = battleQueuePostRepository.getAll()
        val found = hashSetOf<Long>()
        for (it in posts) {
            try {
                if (found.contains(it.id)) continue

                val battle = battleService.findBattle(it) ?: continue
                val otherId = if (battle.goldPost!!.id == it.id) battle.redPost!!.id else battle.goldPost!!.id

                found.add(otherId!!)
            } catch (_: Exception) {}
        }
    }
}