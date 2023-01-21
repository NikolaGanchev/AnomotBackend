package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.Report
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.FollowRepository
import com.anomot.anomotbackend.repositories.ReportRepository
import com.anomot.anomotbackend.utils.ReportReason
import com.anomot.anomotbackend.utils.ReportType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserModerationService @Autowired constructor(
        private val followRepository: FollowRepository,
        private val userDetailsServiceImpl: UserDetailsServiceImpl,
        private val reportRepository: ReportRepository
) {
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

    fun appeal(appealDto: AppealDto, user: User) {

    }
}