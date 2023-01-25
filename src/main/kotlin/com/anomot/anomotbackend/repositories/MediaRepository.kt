package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MediaRepository: JpaRepository<Media, Long> {
    fun getMediaByPublisher(user: User, pageable: Pageable): List<Media>

    @Modifying
    @Query("delete from Media m where m.publisher = ?1")
    fun deleteByUser(user: User)

    @Query("select m.id from media m where extract(epoch from(now() - m.creation_date)) > ?1" +
            " and not exists(select 1 from post p where p.media_id = m.id)" +
            " and not exists(select 1 from appeal a where a.media_id = m.id)" +
            " and not exists(select 1 from users u where u.avatar_id = m.id)", nativeQuery = true)
    @Modifying
    fun getUnreferencedMediaAfterSeconds(seconds: Int): List<Long>

    @Query("delete from Media m where m.id in (:media)")
    @Modifying
    fun deleteByIds(media: List<Long>)

    @Query("select m.name from Media m where m.id in (:media)")
    fun getNamesByIds(media: List<Long>): List<UUID>

    fun getByName(uuid: UUID): Media?
}