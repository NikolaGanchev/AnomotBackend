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
import java.math.BigInteger

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
        battles.forEach {
            try {
                battleService.finish(it)
                if (it.goldPost != null) {
                    notificationService.sendBattleEndNotification(it.goldPost!!.poster, it)
                }
                if (it.redPost != null) {
                    notificationService.sendBattleEndNotification(it.redPost!!.poster, it)
                }
            } catch (e: Exception) {
                logger.error(e.message)
            }
        }
    }


    @Scheduled(fixedDelay = 5 * 1000)
    @Transactional
    fun findBattles() {
        val matchmade = battleQueuePostRepository.matchmake()
        matchmade.forEach {
            try {
                val firstCandidate = battleQueuePostRepository.getReferenceById(it.get(0, BigInteger::class.java).longValueExact())
                val secondCandidate = battleQueuePostRepository.getReferenceById(it.get(1, BigInteger::class.java).longValueExact())
                battleService.createBattle(firstCandidate, secondCandidate)
            } catch (e: Exception) {
                logger.error(e.message)
            }
        }
    }
}