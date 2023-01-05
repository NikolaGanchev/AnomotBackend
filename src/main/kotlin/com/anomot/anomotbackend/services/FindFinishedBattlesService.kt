package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.repositories.BattleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FindFinishedBattlesService@Autowired constructor(
        val battleRepository: BattleRepository,
        val battleService: BattleService
) {

    @Scheduled(fixedRate = 5 * 1000)
    @Transactional
    fun findFinishedBattles() {
        val battles = battleRepository.getUnprocessedFinishedBattlesAndUpdate()
        battles.forEach {
            battleService.finish(it)
        }
    }
}