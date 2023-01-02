package com.anomot.anomotbackend.entities

import java.io.Serializable
import javax.persistence.*

@Entity
class Follow(
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "followedId")
        val followed: User,
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "followerId")
        val follower: User,
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable