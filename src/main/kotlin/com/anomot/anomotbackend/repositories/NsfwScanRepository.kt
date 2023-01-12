package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.dto.NsfwScans
import com.anomot.anomotbackend.entities.NsfwScan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NsfwScanRepository: JpaRepository<NsfwScan, Long> {

    @Query("select new com.anomot.anomotbackend.dto.NsfwScans(" +
            "(from NsfwScan n where n.type = com.anomot.anomotbackend.utils.NsfwScanType.AVERAGE)," +
            "(from NsfwScan n where n.type = com.anomot.anomotbackend.utils.NsfwScanType.MAX)) " +
            "from NsfwScan nsfw where nsfw.media.name = ?1")
    fun getMaxAndAverageByMediaName(name: UUID): NsfwScans
}