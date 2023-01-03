package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.repositories.BattleQueueRepository
import com.anomot.anomotbackend.repositories.BattleRepository
import com.anomot.anomotbackend.repositories.PostRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BattleService @Autowired constructor(
        private val postRepository: PostRepository,
        private val battleQueueRepository: BattleQueueRepository,
        private val battleRepository: BattleRepository
) {

    fun findBattle() {

    }

    fun queueBattle() {


        findBattle()
    }

    fun finish() {

    }
}