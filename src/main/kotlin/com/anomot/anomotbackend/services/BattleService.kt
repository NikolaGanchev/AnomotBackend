package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class BattleService @Autowired constructor(
        private val battleQueueRepository: BattleQueueRepository,
        private val battleRepository: BattleRepository,
        private val eloService: EloService,
        private val voteRepository: VoteRepository
) {

    @Transactional
    fun findBattle(battleQueuePost: BattleQueuePost): Battle? {
        val candidates = battleQueueRepository.findSimilarByElo(battleQueuePost)

        if (candidates.isEmpty()) return null
        val candidate = candidates[0]

        if (battleQueuePost.post.type != candidate.post.type)
            throw Exception("Candidates for battle don't have a matching post type (if this ever happens, there is a problem in the matchmaking queries)")

        val battle = Battle(battleQueuePost.post,
                candidate.post,
                candidate.post.type,
                totalVotePossibilities = 0,
                finishDate = Date.from(Date().toInstant().plusSeconds(Constants.BATTLE_DURATION.toLong())))

        val savedBattle = battleRepository.save(battle)
        battleQueueRepository.delete(battleQueuePost)
        battleQueueRepository.delete(candidate)

        return savedBattle
    }

    fun queuePostForBattle(post: Post): Battle? {
        val battleQueuePost = battleQueueRepository.save(BattleQueuePost(post))

        return findBattle(battleQueuePost)
    }

    // Automatically adjusts elos and closes the battle
    @Transactional
    fun finish(battle: Battle) {
        if (battle.goldPost == null || battle.redPost == null) {
            return
        }

        val votesGold = voteRepository.countVotesByBattleAndPost(battle, battle.goldPost!!)
        val votesRed = voteRepository.countVotesByBattleAndPost(battle, battle.redPost!!)

        val scoreGold = determineScore(votesGold, votesRed)
        val scoreRed = determineScore(votesRed, votesGold)
        val goldUser = battle.goldPost!!.poster
        val redUser = battle.redPost!!.poster

        if (goldUser == null && redUser != null) {
            if (votesRed >= votesGold) {
                redUser.elo += 30
            } else redUser.elo -= 30
        } else if (goldUser != null && redUser == null) {
            if (votesGold >= votesRed) {
                goldUser.elo += 30
            } else goldUser.elo -= 30
        } else if (goldUser == null && redUser == null) {
            return
        }

        val expected = eloService.getUserProbability(goldUser!!, redUser!!)

        val goldNewElo = eloService.getNextRating(goldUser.elo, expected.goldUserProbability, scoreGold)
        val redNewElo = eloService.getNextRating(redUser.elo, expected.redUserProbability, scoreRed)

        battle.goldPost!!.poster!!.elo = goldNewElo
        battle.redPost!!.poster!!.elo = redNewElo
    }

    private fun determineScore(votes1: Long, votes2: Long): Double {
        return if (votes1 == votes2) EloService.drawScore
        else if (votes1 > votes2) EloService.winScore
        else EloService.loseScore
    }
}