package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class File(
        var originalName: String,
        var name: UUID,
        var threat: String,
        @ManyToOne
        val uploader: User,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable
