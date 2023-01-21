package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.AdminReportDto
import com.anomot.anomotbackend.entities.ReportDecision
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.ReportDecisionRepository
import com.anomot.anomotbackend.repositories.ReportRepository
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
        private val reportDecisionRepository: ReportDecisionRepository
) {
    @Secured("ROLE_ADMIN")
    fun getReports(page: Int): List<AdminReportDto> {
        return reportRepository.getAll(PageRequest.of(page, 10, Sort.by("creationDate"))).map {i->
            AdminReportDto(reportRepository.getReasonsByReportId(i.reportId).toTypedArray(),
                    i.type, i.reporter.id.toString(), i.post?.id.toString(), i.battle?.id.toString(), i.comment?.id.toString(),
                    i.user?.id.toString(), i.decided, i.decision?.decision, i.decision?.decidedBy?.id.toString(), i.decision?.creationDate,
                    i.creationDate, i.reportId.toString())
        }
    }

    @Secured("ROLE_ADMIN")
    fun getUndecidedReports(page: Int): List<AdminReportDto> {
        return reportRepository.getAllByDecidedIsFalse(PageRequest.of(page, 10, Sort.by("creationDate"))).map {i ->
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
}