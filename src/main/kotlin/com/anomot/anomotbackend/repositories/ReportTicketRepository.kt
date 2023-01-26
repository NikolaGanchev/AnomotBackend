package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.*
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*

interface ReportTicketRepository: JpaRepository<ReportTicket, Long> {

    @Query("from ReportTicket rt where rt.post = ?1 or (rt.post = ?1 and rt.battle = ?2) or rt.comment = ?3 or rt.user = ?4")
    fun getByPostOrBattleOrCommentOrUser(post: Post?, battle: Battle?, comment: Comment?, user: User?): ReportTicket?

    @Query("select rt.type as reportType, " +
            "rt.post_id as post," +
            "(select count(l.id) from \"like\" l where l.post_id=rt.post_id) as postLikes, " +
            "rt.battle_id as battle, " +
            "(select count(v.id) from vote v where v.battle_id=rt.battle_id and (v.post_id in (select b.gold_post_id from battle b where b.gold_post_id=rt.post_id))) as goldVotes, " +
            "(select count(v.id) from vote v where v.battle_id=rt.battle_id and (v.post_id in (select b.red_post_id from battle b where b.red_post_id=rt.post_id))) as redVotes, " +
            "rt.comment_id as comment, (select count(c.id) from comment c where c.parent_comment_id=rt.comment_id) as commentResponseCount, " +
            "(select count(cl.id) from comment_like cl where cl.comment_id=rt.comment_id) as commentLikes, " +
            "rt.user_id as \"user\", rt.decided, (select count(rd.id) from report_decision rd where rd.report_ticket_id=rt.id) as decisions, " +
            "rt.creation_date as creationDate, rt.id as id " +
            "from report_ticket rt where rt.decided = false and (rt.post_id is not null or ( rt.battle_id is not null ) and ( rt.post_id is not null ) or rt.comment_id is not null or rt.user_id is not null)", nativeQuery = true)
    fun getAllByDecidedIsFalse(pageable: Pageable): List<ReportTicketIntermediary>

    @Query("select rt.type as reportType, " +
            "rt.post_id as post," +
            "(select count(l.id) from \"like\" l where l.post_id=rt.post_id) as postLikes, " +
            "rt.battle_id as battle, " +
            "(select count(v.id) from vote v where v.battle_id=rt.battle_id and (v.post_id in (select b.gold_post_id from battle b where b.gold_post_id=rt.post_id))) as goldVotes, " +
            "(select count(v.id) from vote v where v.battle_id=rt.battle_id and (v.post_id in (select b.red_post_id from battle b where b.red_post_id=rt.post_id))) as redVotes, " +
            "rt.comment_id as comment, (select count(c.id) from comment c where c.parent_comment_id=rt.comment_id) as commentResponseCount, " +
            "(select count(cl.id) from comment_like cl where cl.comment_id=rt.comment_id) as commentLikes, " +
            "rt.user_id as \"user\", rt.decided, (select count(rd.id) from report_decision rd where rd.report_ticket_id=rt.id) as decisions, " +
            "rt.creation_date as creationDate, rt.id as id " +
            "from report_ticket rt where rt.post_id is not null or ( rt.battle_id is not null ) and ( rt.post_id is not null ) or rt.comment_id is not null or rt.user_id is not null", nativeQuery = true)
    fun getAll(pageable: Pageable): List<ReportTicketIntermediary>

    @Query("update ReportTicket r set r.post = NULL where r.post = ?1")
    @Modifying
    fun setPostToNull(post: Post)

    @Query("delete from ReportTicket r where r.post.id in (select p.id from Post p where p.poster = ?1) or" +
            " r.user = ?1 or r.comment.id in (select c.id from Comment c where c.commenter = ?1)")
    @Modifying
    fun deleteByUser(user: User)
}

interface ReportTicketIntermediary {
    val reportType: Int
    val post: Long?
    val postLikes: Long
    val battle: Long?
    val goldVotes: Long
    val redVotes: Long
    val comment: Long?
    val commentResponseCount: Long
    val commentLikes: Long
    val user: Long?
    val isDecided: Boolean
    val decisions: Long
    val creationDate: Date
    val id: Long
}
