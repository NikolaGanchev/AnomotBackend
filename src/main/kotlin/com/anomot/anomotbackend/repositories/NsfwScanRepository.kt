package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.NsfwScans
import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.NsfwScan
import com.anomot.anomotbackend.entities.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NsfwScanRepository: JpaRepository<NsfwScan, Long> {

    @Query("select new com.anomot.anomotbackend.dto.NsfwScans(" +
            "(select n from NsfwScan n where n.type = com.anomot.anomotbackend.utils.NsfwScanType.AVERAGE and n.media.name = ?1)," +
            "(select n from NsfwScan n where n.type = com.anomot.anomotbackend.utils.NsfwScanType.MAX and n.media.name = ?1)) " +
            "from NsfwScan nsfw where nsfw.media.name = ?1")
    fun getMaxAndAverageByMediaName(name: UUID): NsfwScans
    fun deleteByMedia(media: Media): Long

    @Modifying
    @Query("delete from NsfwScan n where n.media.id in (select m.id from Media m where m.publisher = ?1)")
    fun deleteByUser(user: User)
}