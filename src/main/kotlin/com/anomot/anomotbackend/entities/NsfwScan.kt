package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.NsfwScanType
import java.io.Serializable
import javax.persistence.*

@Entity
class NsfwScan(
        var drawings: Float,
        var hentai: Float,
        var neutral: Float,
        var porn: Float,
        var sexy: Float,
        @Enumerated(EnumType.ORDINAL)
        var type: NsfwScanType,
        @ManyToOne(fetch = FetchType.LAZY)
        var media: Media?,
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable