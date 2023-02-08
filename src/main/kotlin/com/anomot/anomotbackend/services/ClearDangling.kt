package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@ConditionalOnProperty(
        value = ["scaling.is.main"], havingValue = "true"
)
@Component
class ClearDangling@Autowired constructor(
        private val mediaRepository: MediaRepository,
        private val mediaService: MediaService,
        private val nsfwScanRepository: NsfwScanRepository
){

    @Transactional
    @Scheduled(fixedRate = 60 * 60 * 3)
    fun clearMedia() {
        val media = mediaRepository.getUnreferencedMediaAfterSeconds(Constants.APPEAL_PERIOD.toInt())
        nsfwScanRepository.deleteByMedia(media)
        val mediaNames = mediaRepository.getNamesByIds(media)
        mediaRepository.deleteByIds(media)
        mediaNames.forEach {
            mediaService.deleteMediaFromServer(it.toString())
        }
    }
}