package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.AdminReportIntermediate
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.utils.ReportReason
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ReportRepository: JpaRepository<Report, Long> {

    fun getAllByReportTicket(reportTicket: ReportTicket, pageable: Pageable): List<Report>

    fun getAllByReporterAndReportTicketUser(user: User, otherUser: User): List<Report>

    fun existsByReportTicketAndReporterAndReportReason(reportTicket: ReportTicket, user: User, reportReason: ReportReason): Boolean

    fun getAllByReporterAndReportTicketPostAndReportTicketBattle(user: User, post: Post, battle: Battle?): List<Report>

    fun getAllByReporterAndReportTicketChat(user: User, chat: Chat): List<Report>

    @Query("update Report r set r.reporter = NULL where r.reporter = ?1")
    @Modifying
    fun setNullByUser(user: User)

    @Query("delete from Report r where r.reportTicket.id in (select rt.id from ReportTicket rt where rt.post.id in (select p.id from Post p where p.poster = ?1) or " +
            "rt.user = ?1 or rt.comment.id in (select c.id from Comment c where c.commenter = ?1))")
    @Modifying
    fun deleteByUser(user: User)
    fun getAllByReporterAndReportTicketComment(user: User, comment: Comment): List<Report>

    @Query("select new com.anomot.anomotbackend.dto.AdminReportIntermediate(" +
            "r.reportReason, r.other, r.reporter, r.reportTicket.id) " +
            "from Report r where r.reporter = ?1")
    fun getAllByReporter(user: User, pageable: Pageable): List<AdminReportIntermediate>
}