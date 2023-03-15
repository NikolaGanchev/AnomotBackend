package com.anomot.anomotbackend.entities

import java.util.*
import jakarta.persistence.*

@Entity
class RememberMeToken(
        @Column(unique = true)
        var series: String,
        @Column(unique = true)
        var tokenValue: String,
        var email: String,
        var date: Date,
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)