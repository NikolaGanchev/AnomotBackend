package com.anomot.anomotbackend.entities

import java.io.Serializable
import javax.persistence.*

@Entity
class MfaTotpSecret(
        var secret: String,
        @OneToOne(fetch = FetchType.EAGER)
        @JoinColumn(nullable = false, name = "user_id")
        var user: User,
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable