package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.Battle
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.entities.Vote
import com.anomot.anomotbackend.repositories.BattleRepository
import com.anomot.anomotbackend.repositories.VoteRepository
import com.anomot.anomotbackend.utils.Constants
import io.jsonwebtoken.InvalidClaimException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.security.Key
import javax.crypto.SecretKey

@Service
class VoteService @Autowired constructor(
        @Value("\${jwt.private-key}") private val secretKeyString: String,
        private val voteRepository: VoteRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val battleRepository: BattleRepository
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyString))

    fun vote(user: User, voteJWT: String, forId: String): VotedBattleDto? {
        try {
            val jws = Jwts.parserBuilder()
                    .requireAudience(user.id.toString())
                    .require("action", "vote")
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(voteJWT)

            val battle = battleRepository.getByIdAndFinishedFalse(jws.body.subject.toLong()) ?: return null

            val post = if (battle.goldPost?.id == forId.toLong()) battle.goldPost
                        else if (battle.redPost?.id == forId.toLong()) battle.redPost else null

            if (post == null || post.poster.id == user.id) return null

            if (voteRepository.existsByBattleAndVoter(battle, user)) return null

            voteRepository.save(Vote(battle, post, user))

            val it = voteRepository.getByVoterAndBattle(user, battle)
            return createVotedBattleFromIntermediate(it)
        }
        catch (exception: JwtException) {
            return null
        }
        catch (exception: InvalidClaimException) {
            return null
        }
        catch (exception: NumberFormatException) {
            return null
        }
    }

    fun genJwtKey(): String {
        val key: Key = Keys.secretKeyFor(SignatureAlgorithm.HS256)
        return Encoders.BASE64.encode(key.encoded)
    }

    fun getVoteHistory(user: User, page: Int): List<VotedBattleDto> {
        return voteRepository.getAllByVoter(user,
                PageRequest.of(page, Constants.VOTE_PAGE, Sort.by("creationDate").descending())).map {
            createVotedBattleFromIntermediate(it)
        }
    }

    fun createVotedBattleFromIntermediate(it: VotedBattleIntermediate): VotedBattleDto {
        val votedPost = it.vote.post
        val otherPost = if (it.vote.battle.goldPost == it.vote.post) it.vote.battle.redPost else it.vote.battle.goldPost
        val votedUserDto = if (votedPost?.poster != null) userDetailsServiceImpl.getAsDto(votedPost.poster) else null

        val otherUserDto = if (it.canSeeOtherUser && otherPost?.poster != null) userDetailsServiceImpl.getAsDto(otherPost.poster) else null

        return VotedBattleDto(
                votedPost = if (votedPost == null) null else PostDto(votedPost.type,
                        votedPost.text,
                        if (votedPost.media != null) MediaDto(votedPost.media!!.mediaType, votedPost.media!!.name.toString()) else null,
                        votedUserDto,
                        null,
                        null,
                        votedPost.creationDate,
                        votedPost.id.toString()),
                otherPost = if (otherPost == null) null else BattlePostDto(otherPost.type,
                        otherPost.text,
                        if (otherPost.media != null) MediaDto(otherPost.media!!.mediaType, otherPost.media!!.name.toString()) else null,
                        otherPost.id.toString(),
                        otherUserDto),
                votesForVoted = it.votesForVoted,
                votesForOther = it.votesForOther,
                isFinished = it.vote.battle.finished
        )
    }

    fun genVoteJwt(user: User, battle: Battle): String {
        return Jwts.builder()
                .setSubject(battle.id.toString())
                .setAudience(user.id.toString())
                .setExpiration(battle.finishDate)
                .claim("action", "vote")
                .signWith(secretKey)
                .compact()
    }
}