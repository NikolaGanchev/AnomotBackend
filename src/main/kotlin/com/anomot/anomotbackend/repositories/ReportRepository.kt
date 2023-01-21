package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.SingleReportDto
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.utils.ReportReason
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ReportRepository: JpaRepository<Report, Long> {

    @Query("select r from Report r where r.reporter = ?1 and r.post = ?2 and r.battle = NULL")
    fun getByReporterAndPost(user: User, post: Post): List<Report>

    @Query("select r.reportId from Report r where r.reporter = ?1 and r.post = ?2 and r.battle = NULL")
    fun getIdByReporterAndPost(user: User, post: Post): UUID?

    @Query("select count(r) > 0 from Report r where r.reporter = ?1 and r.post = ?2 and r.battle = NULL and r.reportReason = ?3")
    fun existByReporterAndPostAndReason(user: User, post: Post, reason: ReportReason): Boolean

    @Query("select r from Report r where r.reporter = ?1 and r.post = ?2 and r.battle = ?3")
    fun getByReporterAndPostAndBattle(user: User, post: Post, battle: Battle): List<Report>

    @Query("select r.reportId from Report r where r.reporter = ?1 and r.post = ?2 and r.battle = ?3")
    fun getIdByReporterAndPostAndBattle(user: User, post: Post, battle: Battle): UUID?

    @Query("select count(r) > 0 from Report r where r.reporter = ?1 and r.post = ?2 and r.battle = ?3 and r.reportReason = ?4")
    fun existByReporterAndPostAndBattleAndReason(user: User, post: Post, battle: Battle, reason: ReportReason): Boolean

    @Query("select r from Report r where r.reporter = ?1 and r.user = ?2")
    fun getByReporterAndUser(reporter: User, user: User): List<Report>

    @Query("select r.reportId from Report r where r.reporter = ?1 and r.user = ?2")
    fun getIdByReporterAndUser(reporter: User, user: User): UUID?

    @Query("select count(r) > 0 from Report r where r.reporter = ?1 and r.user = ?2 and r.reportReason = ?3")
    fun existByReporterAndUserAndReason(reporter: User, user: User, reason: ReportReason): Boolean

    @Query("select r from Report r where r.reporter = ?1 and r.comment = ?2")
    fun getByReporterAndComment(reporter: User, comment: Comment): List<Report>

    @Query("select r.reportId from Report r where r.reporter = ?1 and r.comment = ?2")
    fun getIdByReporterAndComment(reporter: User, comment: Comment): UUID?

    @Query("select count(r) > 0 from Report r where r.reporter = ?1 and r.comment = ?2 and r.reportReason = ?3")
    fun existByReporterAndCommentAndReason(reporter: User, comment: Comment, reason: ReportReason): Boolean

    @Query("select r " +
            "from Report r where r.id = (select min(r1.id) from Report r1 group by r1.reportId)")
    fun getAll(pageable: Pageable): List<Report>

    @Query("select r " +
            "from Report r where r.id in (select min(r1.id) from Report r1 group by r1.reportId) and r.decided = false")
    fun getAllByDecidedIsFalse(pageable: Pageable): List<Report>

    @Query("select new com.anomot.anomotbackend.dto.SingleReportDto(r.reportReason, r.other) " +
            "from Report r where r.reportId = ?1")
    fun getReasonsByReportId(reportId: UUID): List<SingleReportDto>

    @Modifying
    @Query("update Report r set r.decision = ?2, r.decided = true where r.reportId = ?1")
    fun setDecisionById(reportId: UUID, decision: ReportDecision): Int
}