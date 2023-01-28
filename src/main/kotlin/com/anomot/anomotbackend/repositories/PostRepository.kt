package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.PostWithLikeNumber
import com.anomot.anomotbackend.dto.PostWithLikes
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PostRepository: JpaRepository<Post, Long> {


    @Query("select new com.anomot.anomotbackend.dto.PostWithLikes(p, " +
            "(select count(l) from Like l where l.post = p), " +
            // TODO could use exists here
            "(select count(l) > 0 from Like l where l.post = p and l.likedBy = ?1)) " +
            "from Post p where p.poster = ?1 ")
    fun findAllByPosterSelf(poster: User, pageable: Pageable): List<PostWithLikes>


    /*
    To see a post:
    you need to be the poster OR
    the post needs to not be in the battle queue AND
        there needs to be no battles with the post OR
            the battle is finished OR there exists a vote from the user for the post OR the user is in the battle
     */
    @Query("select new com.anomot.anomotbackend.dto.PostWithLikes(p, " +
            "(select count(l) from Like l where l.post = p), " +
            "(select count(l) > 0 from Like l where l.post = p and l.likedBy = ?1)) " +
            "from Post p " +
            "where p.poster = ?1 and (p.poster = ?2 " +
            // Check if in battle queue
            "or (not exists(from BattleQueuePost bp where bp.post = p) " +
            // Check if battle exists
            "and (not exists(from Battle b where b.goldPost = p or b.redPost = p) " +
            // Check if battle is finished
            "or exists(from Battle b where (b.goldPost = p or b.redPost = p) and b.finished = true)" +
            // Check if there is a vote from the user
            "or exists(from Vote v, Battle b where v.battle = b and v.post = p and v.voter = ?2 and (b.goldPost = p or b.redPost = p))" +
            // Check if the user is in the battle
            "or exists(from Battle b where (b.goldPost = p or b.redPost = p) and (b.goldPost.poster = ?2 or b.redPost.poster = ?2))))) ")
    fun findAllByPosterOther(poster: User, fromUser: User, pageable: Pageable): List<PostWithLikes>

    @Query("select count(p) > 0 " +
            "from Post p " +
            "where p.poster = ?1 and (p.poster = ?2 " +
            // Check if in battle queue
            "or (not exists(from BattleQueuePost bp where bp.post = p) " +
            // Check if battle exists
            "and (not exists(from Battle b where b.goldPost = p or b.redPost = p) " +
            // Check if battle is finished
            "or exists(from Battle b where (b.goldPost = p or b.redPost = p) and b.finished = true)" +
            // Check if there is a vote from the user
            "or exists(from Vote v, Battle b where v.battle = b and v.post = p and v.voter = ?2 and (b.goldPost = p or b.redPost = p))" +
            // Check if the user is in the battle
            "or exists(from Battle b where (b.goldPost = p or b.redPost = p) and (b.goldPost.poster = ?2 or b.redPost.poster = ?2))))) ")
    fun canSeePost(poster: User, fromUser: User): Boolean

    fun deleteByIdAndPoster(id: Long, poster: User): Long

    @Query("select new com.anomot.anomotbackend.dto.PostWithLikes(p, " +
            "(select count(l) from Like l where l.post = p), " +
            "(select count(l) > 0 from Like l where l.post = p and l.likedBy = ?1)) " +
            "from Post p, Follow f where p.poster = f.followed and f.follower = ?1")
    fun getFeed(user: User, pageable: Pageable): List<PostWithLikes>

    @Query("select p.poster from Post p where p.id = ?1")
    fun findPosterById(id: Long): User

    @Query("select new com.anomot.anomotbackend.dto.PostWithLikes(p, " +
            "(select count(l) from Like l where l.post = p), " +
            // TODO could use exists here
            "(select count(l) > 0 from Like l where l.post = p and l.likedBy = ?1)) " +
            "from Post p where p = ?2")
    fun getWithLikesByPostId(user: User, post: Post): PostWithLikes?

    @Modifying
    @Query("delete from Post p where p.poster = ?1")
    fun deleteByUser(user: User)

    @Query("select new com.anomot.anomotbackend.dto.PostWithLikeNumber(p, " +
            "(select count(l) from Like l where l.post = p)) " +
            "from Post p where p.poster = ?1")
    fun getAllByPoster(user: User, page: Pageable): List<PostWithLikeNumber>

    @Query("select count(l) from Like l join Post p where l.post = p and p.poster = ?1")
    fun getLikesByPost(): Long
}