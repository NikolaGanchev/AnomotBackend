package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.*
import io.jsonwebtoken.InvalidClaimException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey
import javax.transaction.Transactional

@Service
class UserModerationService @Autowired constructor(
        @Value("\${jwt.private-key}") private val secretKeyString: String,
        private val followRepository: FollowRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val reportRepository: ReportRepository,
        private val reportTicketRepository: ReportTicketRepository,
        private val mediaRepository: MediaRepository,
        private val appealRepository: AppealRepository,
        private val reportDecisionRepository: ReportDecisionRepository
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyString))

    fun report(userReportDto: UserReportDto, user: User): Boolean {
        val otherUser = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(userReportDto.userId) ?: return false

        if (!(followRepository.canSeeAccount(user, otherUser) or followRepository.existsFollowByFollowerAndFollowed(user, otherUser))) return false

        val reportReason = ReportReason.from(userReportDto.reason)

        return report(reportReason,
                ReportType.USER,
                userReportDto.other,
                user, null, null, null,
                otherUser, null,
                Constants.USER_REPORT_COOLDOWN)
    }

    @Transactional
    fun report(reason: ReportReason,
               type: ReportType,
               other: String?,
               reporter: User,
               post: Post?,
               battle: Battle?,
               comment: Comment?,
               user: User?,
               chat: Chat?,
               decisionCooldown: Long): Boolean {
        var reportTicket = reportTicketRepository.getByPostOrBattleOrCommentOrUserOrChat(post, battle, comment, user, chat)

        if (reportTicket == null) {
            reportTicket = reportTicketRepository.save(ReportTicket(type, post, battle, comment, user, chat,
                    false))
            reportTicketRepository.flush()
        } else {
            if (reportRepository.existsByReportTicketAndReporterAndReportReason(reportTicket, reporter, reason)) {
                return false
            }

            val latestDecision = reportDecisionRepository.getLatest(reportTicket.id!!)
            if (latestDecision != null) {
                if ((Instant.now().epochSecond -
                                latestDecision.creationDate.toInstant().epochSecond) > decisionCooldown) {
                    reportTicket.decided = false
                }
            }
        }

        try {
            reportRepository.save(Report(
                    reporter,
                    reportTicket,
                    reason,
                    other
            ))
        } catch (e: ConstraintViolationException) {
            return false
        }

        return true
    }

    fun getReport(user: User, userId: String): ReportDto? {
        val otherUser = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(userId) ?: return null

        val reports = reportRepository.getAllByReporterAndReportTicketUser(user, otherUser)

        val singleReportedDtos = reports.map {
            SingleReportDto(it.reportReason, it.other)
        }.toTypedArray()

        return ReportDto(singleReportedDtos, ReportType.USER)
    }

    fun getAppeals(user: User, page: Int): List<AppealDto> {
        return appealRepository.getAllByAppealedBy(user, PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            AppealDto(it.reason, it.objective, it.media.name.toString())
        }
    }

    fun generateAppealJwt(user: User, mediaId: String, reason: AppealReason, objective: AppealObjective): String {
        return Jwts.builder()
                .setSubject(mediaId)
                .setAudience(user.id.toString())
                .setExpiration(Date.from(Instant.now().plusSeconds(Constants.APPEAL_PERIOD)))
                .claim("action", "appeal")
                .claim("reason", reason.name)
                .claim("objective", objective.name)
                .signWith(secretKey)
                .compact()
    }

    fun appeal(appealUploadDto: AppealUploadDto, user: User): Boolean {
        try {
            val jws = Jwts.parserBuilder()
                    .requireAudience(user.id.toString())
                    .require("action", "appeal")
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(appealUploadDto.jwt)

            val reason = AppealReason.valueOf(jws.body["reason", String::class.java])
            val objective = AppealObjective.valueOf(jws.body["objective", String::class.java])
            val media = mediaRepository.getByName(UUID.fromString(jws.body.subject)) ?: return false

            appealRepository.save(Appeal(user, reason, objective, media))
        }
        catch (exception: JwtException) {
            return false
        }
        catch (exception: InvalidClaimException) {
            return false
        }
        catch (exception: NumberFormatException) {
            return false
        }
        catch (exception: IllegalArgumentException) {
            return false
        }
        catch (e: ConstraintViolationException) {
            return false
        }

        return true
    }

    fun getAppeal(user: User, id: String): AppealDto? {
        val appeal = getAppealReferenceByIdStringUnsafe(id) ?: return null

        if (appeal.appealedBy.id != user.id) return null

        return AppealDto(appeal.reason, appeal.objective, appeal.media.name.toString())
    }

    private fun getAppealReferenceByIdStringUnsafe(id: String): Appeal? {
        val idLong = id.toLongOrNull() ?: return null
        if (!appealRepository.existsById(idLong)) return null
        return appealRepository.getReferenceById(idLong)
    }
}