package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.lang.NumberFormatException
import java.util.*
import javax.transaction.Transactional

@Service
class BattleService @Autowired constructor(
        private val battleQueueRepository: BattleQueueRepository,
        private val battleRepository: BattleRepository,
        private val eloService: EloService,
        private val voteRepository: VoteRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val voteService: VoteService
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
        val goldUser = battle.goldPost?.poster
        val redUser = battle.redPost?.poster

        if (goldUser == null && redUser != null) {
            redUser.elo += 15
            return
        } else if (goldUser != null && redUser == null) {
            goldUser.elo += 15
            return
        } else if (goldUser == null && redUser == null) {
            return
        }

        val votesGold = voteRepository.countVotesByBattleAndPost(battle, battle.goldPost!!)
        val votesRed = voteRepository.countVotesByBattleAndPost(battle, battle.redPost!!)

        val scoreGold = determineScore(votesGold, votesRed)
        val scoreRed = determineScore(votesRed, votesGold)

        val expected = eloService.getUserProbability(goldUser!!, redUser!!)

        val goldNewElo = eloService.getNextRating(goldUser.elo, scoreGold, expected.goldUserProbability)
        val redNewElo = eloService.getNextRating(redUser.elo, scoreRed, expected.redUserProbability)

        battle.goldPost!!.poster.elo = goldNewElo
        battle.redPost!!.poster.elo = redNewElo
    }

    private fun determineScore(votes1: Long, votes2: Long): Double {
        return if (votes1 == votes2) EloService.drawScore
        else if (votes1 > votes2) EloService.winScore
        else EloService.loseScore
    }

    fun getBattles(user: User, page: Int): List<SelfBattleDto> {
        return battleRepository.getAllBattlesByUser(user,
                PageRequest.of(page, Constants.BATTLE_PAGE, Sort.by("creationDate").descending())).map {
            getSelfBattleDtoFromBattleIntermediate(it, user)
        }
    }

    @Transactional
    fun getBattle(user: User, page: Int): BattleDto? {
        val battle = battleRepository.getBattle(user.id!!, PageRequest.of(page, 1))[0] ?: return null

        if (battle.goldPost == null || battle.redPost == null) return null

        battle.totalVotePossibilities += 1

        return BattleDto(
             BattlePostDto(
                     battle.goldPost!!.type,
                     battle.goldPost!!.text,
                    if (battle.goldPost!!.media != null) MediaDto(battle.goldPost!!.media!!.mediaType, battle.goldPost!!.media!!.name.toString()) else null,
                    battle.goldPost!!.id.toString(), null),
             BattlePostDto(
                     battle.redPost!!.type,
                     battle.redPost!!.text,
                     if (battle.redPost!!.media != null) MediaDto(battle.redPost!!.media!!.mediaType, battle.redPost!!.media!!.name.toString()) else null,
                    battle.redPost!!.id.toString(), null),
                    voteService.genVoteJwt(user, battle)
        )
    }

    fun getPostsInQueue(user: User, page: Int): List<PostDto> {
        return battleQueueRepository.getAllByPostPoster(user, user, PageRequest.of(page, Constants.POST_PAGE, Sort.by("post.creationDate"))).map {
            PostDto(it.post!!.type,
                    it.post.text,
                    if (it.post.media != null) MediaDto(it.post.media!!.mediaType, it.post.media!!.name.toString()) else null,
                    userDetailsServiceImpl.getAsDto(it.post.poster),
                    it.likes,
                    it.hasUserLiked,
                    it.post.creationDate,
                    it.post.id.toString())
        }
    }

    fun getSelfBattleDtoFromBattleIntermediate(battleIntermediate: BattleIntermediate, user: User): SelfBattleDto {
        val battle = battleIntermediate.battle

        val selfPost = if (battle.goldPost?.poster?.id == user.id) {
            battle.goldPost
        } else {
            battle.redPost
        }

        val enemyPost = if (battle.goldPost?.poster?.id == user.id) {
            battle.redPost
        } else {
            battle.goldPost
        }

        return SelfBattleDto(
                if (selfPost == null) null else PostDto(selfPost.type,
                        selfPost.text,
                        if (selfPost.media != null) MediaDto(selfPost.media!!.mediaType, selfPost.media!!.name.toString()) else null,
                        userDetailsServiceImpl.getAsDto(selfPost.poster),
                        null,
                        null,
                        selfPost.creationDate,
                        selfPost.id.toString()),
                if (enemyPost == null) null else PostDto(enemyPost.type,
                        enemyPost.text,
                        if (enemyPost.media != null) MediaDto(enemyPost.media!!.mediaType, enemyPost.media!!.name.toString()) else null,
                        userDetailsServiceImpl.getAsDto(enemyPost.poster),
                        null,
                        null,
                        enemyPost.creationDate,
                        enemyPost.id.toString()),
                battleIntermediate.votesForSelf,
                battleIntermediate.votesForOther,
                battle.finished,
                battle.finishDate!!)
    }


    fun getSelfBattle(user: User, battleId: Long): SelfBattleDto? {
        val battleIntermediate = battleRepository.getBattleByUser(user, battleId) ?: return null

        return getSelfBattleDtoFromBattleIntermediate(battleIntermediate, user)
    }

    fun getBattleReferenceFromIdUnsafe(battleId: String): Battle? {
        return try {
            return if (battleRepository.existsById(battleId.toLong())) {
                battleRepository.getReferenceById(battleId.toLong())
            } else null
        } catch(numberFormatException: NumberFormatException) {
            null
        }
    }

    fun getBattleById(user: User, battleId: String): Any? {
        val battleLongId = try {
            battleId.toLong()
        } catch (numberFormatException: NumberFormatException) {
            return null
        }

        val battle = battleRepository.getReferenceById(battleLongId)

        if (!battleRepository.canSeeBattle(user, battle)) return null

        return if (battle.goldPost?.poster?.id == user.id || battle.redPost?.poster?.id == user.id) {
            getSelfBattle(user, battleLongId)
        } else {
            voteService.createVotedBattleFromIntermediate(voteRepository.getByVoterAndBattle(user, battle))
        }
    }
}