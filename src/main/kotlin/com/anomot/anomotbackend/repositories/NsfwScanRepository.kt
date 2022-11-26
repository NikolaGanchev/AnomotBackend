package com.anomot.anomotbackend.repositories

import com.anomot.anomotbackend.entities.NsfwScan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NsfwScanRepository: JpaRepository<NsfwScan, Long> {}