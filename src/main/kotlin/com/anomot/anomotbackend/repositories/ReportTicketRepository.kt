package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.utils.ReportType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*

interface ReportTicketRepository: JpaRepository<ReportTicket, Long> {

    fun getByPostOrBattleOrCommentOrUser(post: Post?, battle: Battle?, comment: Comment?, user: User?): ReportTicket?

    @Query("select new com.anomot.anomotbackend.repositories.ReportTicketIntermediary(r.type, " +
            "r.post, (select count(l) from Like l where l.post = r.post)," +
            "r.battle, (select count(v) from Vote v where v.battle = r.battle and v.post = r.battle.goldPost), (select count(v) from Vote v where v.battle = r.battle and v.post = r.battle.redPost)," +
            "r.comment, (select count(c) from Comment c where c.parentComment = r.comment), (select count(l) from CommentLike l where l.comment = r.comment)," +
            "r.user, r.decided, (select count(d) from ReportDecision d where d.reportTicket = r), " +
            "r.creationDate, r.id) " +
            "from ReportTicket r where r.decided = false and (r.post is not null or r.battle is not null or r.comment is not null or r.user is not null)")
    fun getAllByDecidedIsFalse(pageable: Pageable): List<ReportTicketIntermediary>

    @Query("select new com.anomot.anomotbackend.repositories.ReportTicketIntermediary(r.type, " +
            "r.post, (select count(l) from Like l where l.post = r.post)," +
            "r.battle, (select count(v) from Vote v where v.battle = r.battle and v.post = r.battle.goldPost), (select count(v) from Vote v where v.battle = r.battle and v.post = r.battle.redPost)," +
            "r.comment, (select count(c) from Comment c where c.parentComment = r.comment), (select count(l) from CommentLike l where l.comment = r.comment)," +
            "r.user, r.decided, (select count(d) from ReportDecision d where d.reportTicket = r), " +
            "r.creationDate, r.id) " +
            "from ReportTicket r where (r.post is not null or r.battle is not null or r.comment is not null or r.user is not null)")
    fun getAll(pageable: Pageable): List<ReportTicketIntermediary>

    @Query("update ReportTicket r set r.post = NULL where r.post = ?1")
    @Modifying
    fun setPostToNull(post: Post)

    @Query("delete from ReportTicket r where r.post.id in (select p.id from Post p where p.poster = ?1) or" +
            " r.user = ?1 or r.comment.id in (select c.id from Comment c where c.commenter = ?1)")
    @Modifying
    fun deleteByUser(user: User)
}

data class ReportTicketIntermediary(
        val reportType: ReportType,
        val post: Post?,
        val postLikes: Long,
        val battle: Battle?,
        val goldVotes: Long,
        val redVotes: Long,
        val comment: Comment?,
        val commentResponseCount: Long,
        val commentLikes: Long,
        val user: User?,
        val isDecided: Boolean,
        val decisions: Long,
        val creationDate: Date,
        val id: Long,
)
