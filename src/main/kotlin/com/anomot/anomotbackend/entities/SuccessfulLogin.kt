package com.anomot.anomotbackend.entities

import java.util.*
import jakarta.persistence.*

@Entity
class SuccessfulLogin(
    val city: String?,
    val country: String?,
    val deviceType: String?,
    val platform: String?,
    val platformVersion: String?,
    val browser: String?,
    val browserVersion: String?,
    var date: Date? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)