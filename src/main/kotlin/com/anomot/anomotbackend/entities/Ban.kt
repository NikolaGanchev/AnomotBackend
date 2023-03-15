package com.anomot.anomotbackend.entities

import java.util.Date
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class Ban(
        @ManyToOne
        val user: User,
        val until: Date,
        @ManyToOne
        val bannedBy: User?,
        val reason: String,
        val creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)