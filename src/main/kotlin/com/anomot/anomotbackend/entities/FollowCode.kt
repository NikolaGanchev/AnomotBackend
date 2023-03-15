package com.anomot.anomotbackend.entities

import com.anomot.anomotbackend.utils.Constants
import java.io.Serializable
import java.util.*
import jakarta.persistence.*

@Entity
class FollowCode(
        @ManyToOne
        @JoinColumn(unique = true)
        val user: User,
        @Column(length = Constants.FOLLOW_CODE_LENGTH, unique = true)
        var code: String,
        var creationDate: Date,
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable