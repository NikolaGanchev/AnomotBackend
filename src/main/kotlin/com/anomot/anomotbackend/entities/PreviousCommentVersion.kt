package com.anomot.anomotbackend.entities

import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
class PreviousCommentVersion (
        @Column(columnDefinition="TEXT")
        val text: String,
        @ManyToOne
        val comment: Comment,
        var creationDate: Date = Date(),
        @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
): Serializable