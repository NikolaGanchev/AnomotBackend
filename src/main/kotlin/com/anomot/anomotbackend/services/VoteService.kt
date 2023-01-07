package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.VoteRepository
import com.anomot.anomotbackend.security.CustomUserDetails
import com.anomot.anomotbackend.utils.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class VoteService @Autowired constructor(
        private val voteRepository: VoteRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl
) {

    fun vote(user: CustomUserDetails, voteJWT: String, forId: String) {
        //TODO
    }

    fun getVoteHistory(user: User, page: Int): List<VotedBattleDto> {
        return voteRepository.getAllByVoter(user, PageRequest.of(page, Constants.VOTE_PAGE)).map {
            val votedPost = it.vote.post
            val otherPost = if (it.vote.battle.goldPost == it.vote.post) it.vote.battle.redPost else it.vote.battle.goldPost
            val votedUserDto = if (votedPost?.poster != null) userDetailsServiceImpl.getAsDto(votedPost.poster!!) else null

            VotedBattleDto(
                    votedPost = if (votedPost == null) null else PostDto(votedPost.type,
                            votedPost.text,
                            if (votedPost.media != null) MediaDto(votedPost.media!!.mediaType, votedPost.media!!.name.toString()) else null,
                            votedUserDto,
                            0,
                            votedPost.id.toString()),
                    otherPost = if (otherPost == null) null else BattlePostDto(otherPost.type,
                            otherPost.text,
                            if (otherPost.media != null) MediaDto(otherPost.media!!.mediaType, otherPost.media!!.name.toString()) else null,
                            otherPost.id.toString()),
                    votesForVoted = it.votesForVoted,
                    votesForOther = it.votesForOther,
                    isFinished = it.vote.battle.finished
            )
        }
    }
}