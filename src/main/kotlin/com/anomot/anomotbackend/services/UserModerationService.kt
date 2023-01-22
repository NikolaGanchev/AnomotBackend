package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.Appeal
import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.Report
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.*
import io.jsonwebtoken.InvalidClaimException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@Service
class UserModerationService @Autowired constructor(
        @Value("\${jwt.private-key}") private val secretKeyString: String,
        private val followRepository: FollowRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val reportRepository: ReportRepository,
        private val mediaRepository: MediaRepository,
        private val appealRepository: AppealRepository
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyString))

    fun report(userReportDto: UserReportDto, user: User): Boolean {
        val otherUser = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(userReportDto.userId) ?: return false

        if (!(followRepository.canSeeAccount(user, otherUser) or followRepository.existsFollowByFollowerAndFollowed(user, otherUser))) return false

        if (reportRepository.existByReporterAndUserAndReason(user, otherUser, ReportReason.from(userReportDto.reason))) return false

        val reportId = reportRepository.getIdByReporterAndUser(user, otherUser) ?: UUID.randomUUID()

        reportRepository.save(Report(
                user,
                ReportType.USER,
                null,
                null,
                null,
                otherUser,
                ReportReason.from(userReportDto.reason),
                userReportDto.other,
                reportId,
                false,
                null
        ))

        return true
    }

    fun getReport(user: User, userId: String): ReportDto? {
        val otherUser = userDetailsServiceImpl.getUserReferenceFromIdUnsafe(userId) ?: return null

        val reports = reportRepository.getByReporterAndUser(user, otherUser)

        val singleReportedDtos = reports.map {
            SingleReportDto(it.reportReason, it.other)
        }.toTypedArray()

        return ReportDto(singleReportedDtos, ReportType.USER)
    }

    fun getAppeals(user: User, page: Int): List<AppealDto> {
        return appealRepository.getAllByAppealedBy(PageRequest.of(page, 10, Sort.by("creationDate").descending())).map {
            AppealDto(it.reason, it.objective, it.media.name.toString())
        }
    }

    fun generateAppealJwt(user: User, media: Media, reason: AppealReason, objective: AppealObjective): String? {
        return Jwts.builder()
                .setSubject(media.name.toString())
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

        return true
    }
}