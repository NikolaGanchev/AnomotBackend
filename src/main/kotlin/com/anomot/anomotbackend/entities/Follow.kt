package com.anomot.anomotbackend.entities

import java.io.Serializable
import javax.persistence.*

@Table(
        uniqueConstraints=[UniqueConstraint(columnNames=arrayOf("followed_id", "follower_id"))]
)
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