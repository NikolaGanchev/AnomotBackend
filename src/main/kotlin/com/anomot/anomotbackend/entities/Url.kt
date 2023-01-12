package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.Constants
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class Url(
        @Column(columnDefinition="TEXT")
        var url: String,
        @Column(length = Constants.URL_LENGTH, unique = true)
        var inAppUrl: String,
        @ManyToOne
        val publisher: User?,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable