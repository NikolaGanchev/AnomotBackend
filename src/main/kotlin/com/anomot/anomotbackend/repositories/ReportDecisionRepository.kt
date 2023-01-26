package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.ReportDecision
import com.anomot.anomotbackend.entities.ReportTicket
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ReportDecisionRepository: JpaRepository<ReportDecision, Long> {

    @Query("select * from report_decision r where r.report_ticket_id = ?1 order by r.creation_date desc limit 1", nativeQuery = true)
    fun getLatest(reportTicketId: Long): ReportDecision?

    fun getAllByReportTicket(reportTicket: ReportTicket, pageable: Pageable): List<ReportDecision>

    @Query("delete from ReportDecision rd where rd.reportTicket.id in (select rt.id from ReportTicket rt where rt.post.id in (select p.id from Post p where p.poster = ?1) or " +
            "rt.user = ?1 or rt.comment.id in (select c.id from Comment c where c.commenter = ?1))")
    @Modifying
    fun deleteByUser(user: User)

    @Query("update ReportDecision rd set rd.decidedBy = NULL where rd.decidedBy = ?1")
    @Modifying
    fun setNullByDecidedBy(user: User)
}