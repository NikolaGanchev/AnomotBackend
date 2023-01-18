package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.CommentIntermediate
import com.anomot.anomotbackend.entities.Battle
import com.anomot.anomotbackend.entities.Comment
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository: JpaRepository<Comment, Long> {
    @Query("select new com.anomot.anomotbackend.dto.CommentIntermediate(c," +
            "(select count(c1) from Comment c1 where c1.parentComment = c), " +
            "(select count(l) from CommentLike l where l.comment = c), " +
            "(select count(l) > 0 from CommentLike l where l.likedBy = ?2 and l.comment = c), " +
            "(select count(f) > 0 from Follow f where f.followed = c.commenter and f.follower = ?2)) " +
            "from CommentLike cl right join cl.comment c where c.parentPost = ?1 " +
            "group by c.id " +
            "order by count(cl) desc, c.creationDate desc")
    fun getAllByParentPost(post: Post, user: User, pageable: Pageable): List<CommentIntermediate>

    @Query("select new com.anomot.anomotbackend.dto.CommentIntermediate(c," +
            "(select count(c1) from Comment c1 where c1.parentComment = c), " +
            "(select count(l) from CommentLike l where l.comment = c), " +
            "(select count(l) > 0 from CommentLike l where l.likedBy = ?2 and l.comment = c), " +
            "(select count(f) > 0 from Follow f where f.followed = c.commenter and f.follower = ?2)) " +
            "from CommentLike cl right join cl.comment c where c.parentBattle = ?1 " +
            "group by c.id " +
            "order by count(cl) desc, c.creationDate desc")
    fun getAllByParentBattle(battle: Battle, user: User, pageable: Pageable): List<CommentIntermediate>

    @Query("select new com.anomot.anomotbackend.dto.CommentIntermediate(c, " +
            "cast (0 as long)," +
            "(select count(l) from CommentLike l where l.comment = c), " +
            "(select count(l) > 0 from CommentLike l where l.likedBy = ?2 and l.comment = c), " +
            "(select count(f) > 0 from Follow f where f.followed = c.commenter and f.follower = ?2)) " +
            "from CommentLike cl right join cl.comment c where c.parentComment = ?1 " +
            "group by c.id " +
            "order by count(cl) desc, c.creationDate desc")
    fun getAllByParentComment(comment: Comment, user: User, pageable: Pageable): List<CommentIntermediate>

    fun existsByParentComment(comment: Comment): Boolean

    @Query("update Comment c set c.text = '', c.commenter = null, c.isDeleted = true, c.isEdited = false " +
            "where c.commenter = ?1 and c.id = ?2")
    @Modifying
    fun setDeleted(user: User, commentId: Long)

    fun deleteByCommenterAndParentPostPoster(commenter: User, poster: User): Long

    @Query("update Comment c set c.text = '', c.commenter = null, c.isDeleted = true, c.isEdited = false " +
            "where c.commenter = ?1 and exists(select r from Comment r where r.parentComment = c and r.commenter <> ?1)")
    @Modifying
    fun setDeletedByUser(user: User)

    @Modifying
    @Query("delete from Comment c where c.commenter = ?1 " +
            "or c.parentPost.id in (select p.id from Post p where p.poster = ?1)")
    fun deleteByUser(user: User)
}