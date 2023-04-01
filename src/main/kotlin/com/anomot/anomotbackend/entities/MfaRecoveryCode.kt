package com.anomot.anomotbackend.entities

import java.io.Serializable
import javax.persistence.*

@Entity
class MfaRecoveryCode(
        val code: String,
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id")
        val user: User,
      @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable