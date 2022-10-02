package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class PasswordResetToken(
        var resetToken: String,
        @Column(unique = true)
        var identifier: String,
        @OneToOne(fetch = FetchType.EAGER)
        @JoinColumn(nullable = false, name = "user_id")
        var user: User,
        var expiryDate: Date,
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null): Serializable