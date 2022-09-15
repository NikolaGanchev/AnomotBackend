package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.MfaEmailCode
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MfaEmailCodeRepository: CrudRepository<MfaEmailCode, String>