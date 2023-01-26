package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.AppealAction
import com.anomot.anomotbackend.utils.AppealReason
import com.anomot.anomotbackend.utils.MediaType
import com.anomot.anomotbackend.utils.ReportType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class AdminService @Autowired constructor(
        private val reportRepository: ReportRepository,
        private val reportTicketRepository: ReportTicketRepository,
        private val reportDecisionRepository: ReportDecisionRepository,
        private val mediaService: MediaService,
        private val mediaRepository: MediaRepository,
        private val appealRepository: AppealRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val appealDecisionRepository: AppealDecisionRepository,
        private val userDeletionService: UserDeletionService,
        private val battleRepository: BattleRepository,
        private val postRepository: PostRepository,
        private val commentRepository: CommentRepository,
        private val userRepository: UserRepository,
        private val banRepository: BanRepository
) {
    @Secured("ROLE_ADMIN")
    fun getReports(page: Int): List<ReportTicketDto> {
        return reportTicketRepository.getAll(PageRequest.of(page, 10, Sort.by("creation_date").descending())).map {
            getAsReportTicketDto(it)
        }
    }

    private fun getAsReportTicketDto(it: ReportTicketIntermediary): ReportTicketDto {
        val post = if (it.post != null) postRepository.getReferenceById(it.post!!) else null
        val battle = if (it.battle != null) battleRepository.getReferenceById(it.battle!!) else null
        val comment = if (it.comment != null) commentRepository.getReferenceById(it.comment!!) else null
        val user = if (it.user != null) userRepository.getReferenceById(it.user!!) else null

        return ReportTicketDto(ReportType.values().first {i -> i.ordinal == it.reportType},
                if (post != null) asPostDto(post, it.postLikes) else null,
                if (battle != null) AdminBattleDto(
                        if (battle.goldPost != null) asPostDto(battle.goldPost!!, null) else null,
                        if (battle.redPost != null) asPostDto(battle.redPost!!, null) else null,
                        it.goldVotes,
                        it.redVotes,
                        battle.finished,
                        battle.finishDate!!
                ) else null,
                if (comment != null) getAsCommentDto(comment, it.commentLikes, it.commentResponseCount.toInt()) else null,
                if (user != null) userDetailsServiceImpl.getAsDto(user) else null,
                it.isDecided,
                it.decisions.toInt(),
                it.creationDate,
                it.id.toString())
    }

    private fun getAsCommentDto(comment: Comment, commentLikes: Long, responseCount: Int): CommentDto {
        return CommentDto(
                comment.text,
                if (comment.commenter != null) userDetailsServiceImpl.getAsDto(comment.commenter!!) else null,
                comment.isEdited,
                responseCount,
                commentLikes,
                null,
                comment.creationDate,
                comment.id.toString()
        )
    }

    private fun asPostDto(post: Post, likes: Long?): PostDto {
        return PostDto(post.type,
                    post.text,
                    if (post.media != null) MediaDto(post.media!!.mediaType, post.media!!.name.toString()) else null,
                    userDetailsServiceImpl.getAsDto(post.poster),
                    likes,
                    hasUserLiked = null,
                    post.creationDate,
                    post.id.toString())
    }

    @Secured("ROLE_ADMIN")
    fun getUndecidedReports(page: Int): List<ReportTicketDto> {
        return reportTicketRepository.getAllByDecidedIsFalse(PageRequest.of(page, 10, Sort.by("creationDate").ascending())).map {
            getAsReportTicketDto(it)
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun decideReport(user: User, reportTicketId: String, decision: String): Boolean {
        val report = getReportTicketReferenceByIdUnsafe(reportTicketId) ?: return false

        reportDecisionRepository.save(ReportDecision(report, decision, user))
        return true
    }

    private fun getReportTicketReferenceByIdUnsafe(reportTicketId: String): ReportTicket? {
        val ticketId = reportTicketId.toLongOrNull() ?: return null
        if (!reportTicketRepository.existsById(ticketId)) return null

        return reportTicketRepository.getReferenceById(ticketId)
    }

    @Secured("ROLE_ADMIN")
    fun getDecisions(reportTicketId: String, page: Int): List<TicketDecisionDto>? {
        val report = getReportTicketReferenceByIdUnsafe(reportTicketId) ?: return null

        return reportDecisionRepository.getAllByReportTicket(report, PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            TicketDecisionDto(
                    it.decision,
                    userDetailsServiceImpl.getAsDto(it.decidedBy),
                    it.creationDate
            )
        }
    }

    @Secured("ROLE_ADMIN")
    fun getReports(reportTicketId: String, page: Int): List<AdminReportDto>? {
        val report = getReportTicketReferenceByIdUnsafe(reportTicketId) ?: return null

        return reportRepository.getAllByReportTicket(report,  PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            AdminReportDto(it.reportReason, it.other, if (it.reporter != null) userDetailsServiceImpl.getAsDto(it.reporter!!) else null)
        }
    }

    @Secured("ROLE_ADMIN")
    fun getAppeals(page: Int): List<AdminAppealDto> {
        return appealRepository.getAll(PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            AdminAppealDto(
                    userDetailsServiceImpl.getAsDto(it.appealedBy),
                    it.reason,
                    it.objective,
                    it.media.name.toString(),
                    if (it.reason != AppealReason.SIMILAR_FOUND) null else {
                        battleRepository.getSimilarMedia(it.appealedBy, it.media,
                                if (it.media.mediaType == MediaType.VIDEO && it.media.duration != null) {
                                    it.media.duration
                                } else 0f).map {post ->
                            PostDto(post.type,
                                    null,
                                    MediaDto(post.media!!.mediaType, post.media!!.name.toString()),
                                    userDetailsServiceImpl.getAsDto(post.poster),
                                    null,
                                    null,
                                    post.creationDate,
                                    post.id.toString())
                        }
                    },
                    it.decided,
                    if (it.decision != null && it.decision?.decidedBy != null) userDetailsServiceImpl.getAsDto(it.decision!!.decidedBy!!) else null,
                    if (it.decision != null) it.decision!!.decision else null,
                    if (it.decision != null) it.decision!!.explanation else null,
                    it.creationDate,
                    it.id.toString())
        }
    }

    @Secured("ROLE_ADMIN")
    fun getUndecidedAppeals(page: Int): List<AdminAppealDto> {
        return appealRepository.getAllByDecidedIsFalse(PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            AdminAppealDto(
                    userDetailsServiceImpl.getAsDto(it.appealedBy),
                    it.reason,
                    it.objective,
                    it.media.name.toString(),
                    if (it.reason != AppealReason.SIMILAR_FOUND) null else {
                        battleRepository.getSimilarMedia(it.appealedBy, it.media,
                                if (it.media.mediaType == MediaType.VIDEO && it.media.duration != null) {
                                    it.media.duration
                                } else 0f).map {post ->
                            PostDto(post.type,
                                    null,
                                    MediaDto(post.media!!.mediaType, post.media!!.name.toString()),
                                    userDetailsServiceImpl.getAsDto(post.poster),
                                    null,
                                    null,
                                    post.creationDate,
                                    post.id.toString())
                        }
                    },
                    it.decided,
                    if (it.decision != null && it.decision?.decidedBy != null) userDetailsServiceImpl.getAsDto(it.decision!!.decidedBy!!) else null,
                    if (it.decision != null) it.decision!!.decision else null,
                    if (it.decision != null) it.decision!!.explanation else null,
                    it.creationDate,
                    it.id.toString())
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun decideAppeal(user: User, appealId: String, decision: AppealAction, explanation: String): Boolean {
        val appealIdLong = appealId.toLongOrNull() ?: return false
        val savedDecision = appealDecisionRepository.save(AppealDecision(user, decision, explanation))
        appealDecisionRepository.flush()
        if (!appealRepository.existsById(appealIdLong)) return false
        val appeal = appealRepository.getReferenceById(appealIdLong)

        appeal.decision = savedDecision
        appeal.decided = true

        return true
    }


    @Secured("ROLE_ADMIN")
    @Transactional
    fun deleteMedia(mediaId: String): Boolean {
        val media = mediaRepository.getByName(UUID.fromString(mediaId)) ?: return false
        return mediaService.deleteMedia(media)
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun banUser(user: User, userToBan: User, reason: String, until: Date): Boolean {
        if (user.id == userToBan.id) return false
        banRepository.save(Ban(userToBan, until, user, reason))
        userDetailsServiceImpl.expireUserSessions(userToBan)
        return true
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun getBans(user: User, page: Int): List<BanDto> {
        return banRepository.getAllByUser(user, PageRequest.of(page, 12, Sort.by("creationDate").descending())).map {
            BanDto(it.creationDate,
                    it.until,
                    if (it.bannedBy != null) userDetailsServiceImpl.getAsDto(it.bannedBy!!) else null,
                    it.reason)
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun deleteUser(userToDelete: User) {
        userDeletionService.deleteUser(userToDelete)
    }
}