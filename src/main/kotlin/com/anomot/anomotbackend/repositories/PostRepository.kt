package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.PostWithLikes
import com.anomot.anomotbackend.entities.Post
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PostRepository: JpaRepository<Post, Long> {


    @Query("select new com.anomot.anomotbackend.dto.PostWithLikes(p, " +
            "(select count(l) from Like l where l.post = p), " +
            "(select count(l) > 0 from Like l where l.post = p and l.likedBy = ?1)) " +
            "from Post p where p.poster = ?1")
    fun findAllByPoster(poster: User, pageable: Pageable): List<PostWithLikes>

    fun deleteByIdAndPoster(id: Long, poster: User): Long

    @Query("select new com.anomot.anomotbackend.dto.PostWithLikes(p, " +
            "(select count(l) from Like l where l.post = p), " +
            "(select count(l) > 0 from Like l where l.post = p and l.likedBy = ?1)) " +
            "from Post p, Follow f where p.poster = f.followed and f.follower = ?1")
    fun getFeed(user: User, pageable: Pageable): List<PostWithLikes>

    @Query("select p.poster from Post p where p.id = ?1")
    fun findPosterById(id: Long): User
}