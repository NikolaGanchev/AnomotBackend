package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.AdminAppealDto
import com.anomot.anomotbackend.dto.AdminReportDto
import com.anomot.anomotbackend.dto.MediaDto
import com.anomot.anomotbackend.dto.PostDto
import com.anomot.anomotbackend.entities.AppealDecision
import com.anomot.anomotbackend.entities.ReportDecision
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.AppealAction
import com.anomot.anomotbackend.utils.AppealReason
import com.anomot.anomotbackend.utils.MediaType
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
        private val reportDecisionRepository: ReportDecisionRepository,
        private val mediaService: MediaService,
        private val mediaRepository: MediaRepository,
        private val appealRepository: AppealRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val appealDecisionRepository: AppealDecisionRepository,
        private val userDeletionService: UserDeletionService,
        private val battleRepository: BattleRepository
) {
    @Secured("ROLE_ADMIN")
    fun getReports(page: Int): List<AdminReportDto> {
        return reportRepository.getAll(PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {i->
            AdminReportDto(reportRepository.getReasonsByReportId(i.reportId).toTypedArray(),
                    i.type, i.reporter.id.toString(), i.post?.id.toString(), i.battle?.id.toString(), i.comment?.id.toString(),
                    i.user?.id.toString(), i.decided, i.decision?.decision, i.decision?.decidedBy?.id.toString(), i.decision?.creationDate,
                    i.creationDate, i.reportId.toString())
        }
    }

    @Secured("ROLE_ADMIN")
    fun getUndecidedReports(page: Int): List<AdminReportDto> {
        return reportRepository.getAllByDecidedIsFalse(PageRequest.of(page, 10, Sort.by("creationDate").ascending())).map {i ->
            AdminReportDto(reportRepository.getReasonsByReportId(i.reportId).toTypedArray(),
                    i.type, i.reporter.id.toString(), i.post?.id.toString(), i.battle?.id.toString(), i.comment?.id.toString(),
                    i.user?.id.toString(), i.decided, i.decision?.decision, i.decision?.decidedBy?.id.toString(), i.decision?.creationDate,
                    i.creationDate, i.reportId.toString())
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun decideReport(user: User, reportId: String, decision: String): Boolean {

        val savedDecision = reportDecisionRepository.save(ReportDecision(decision, user))
        reportDecisionRepository.flush()
        reportRepository.setDecisionById(UUID.fromString(reportId), savedDecision)

        return true
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
    fun banUser(userToBan: User): Boolean {
        return true
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    fun deleteUser(userToDelete: User) {
        userDeletionService.deleteUser(userToDelete)
    }
}