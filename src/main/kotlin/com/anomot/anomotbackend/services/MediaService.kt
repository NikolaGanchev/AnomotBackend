package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.File
import com.anomot.anomotbackend.entities.Media
import com.anomot.anomotbackend.entities.NsfwScan
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.FileRepository
import com.anomot.anomotbackend.repositories.MediaRepository
import com.anomot.anomotbackend.repositories.NsfwScanRepository
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.MediaType
import com.anomot.anomotbackend.utils.NsfwScanType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.*
import javax.transaction.Transactional


@Service
class MediaService @Autowired constructor(
        private val webClient: WebClient,
        private val mediaRepository: MediaRepository,
        private val nsfwScanRepository: NsfwScanRepository,
        private val fileRepository: FileRepository
) {
    data class MediaUploadResult(val media: Media?, val avgNsfwScan: NsfwScan?, val maxNsfwScan: NsfwScan?)
    data class SquareImageSaveResult(val media: Media?, val avgNsfwScan: NsfwScan?)

    @Value("\${environment.media_server_url}")
    private val mediaServerUrl: String? = null

    fun uploadMedia(file: MultipartFile,
                    shouldHash: Boolean = true,
                    shouldNsfwScan: Boolean = true,
                    user: User): MediaUploadResult? {
        val mediaSaveDto = uploadMediaToServer(file, shouldHash, shouldNsfwScan) ?: return null

        return saveMedia(mediaSaveDto, user = user)
    }

    @Transactional
    fun saveMedia(mediaSaveDto: MediaSaveDto, user: User): MediaUploadResult {
        val media = Media(
                name = UUID.fromString(mediaSaveDto.id),
                mediaType = MediaType.valueOf(mediaSaveDto.type.uppercase()),
                duration = mediaSaveDto.duration,
                phash = if (mediaSaveDto.phash != null) { HexFormat.of().parseHex(mediaSaveDto.phash) } else { null },
                publisher = user
        )

        var avgNsfwScan: NsfwScan? = null
        if (mediaSaveDto.avgNsfw != null) {
            avgNsfwScan = createNsfwScan(mediaSaveDto.avgNsfw, NsfwScanType.AVERAGE)
        }

        var maxNsfwScan: NsfwScan? = null
        if (mediaSaveDto.maxNsfw != null) {
            maxNsfwScan = createNsfwScan(mediaSaveDto.maxNsfw, NsfwScanType.MAX)
        }

        val savedMedia = mediaRepository.saveAndFlush(media)

        var savedAvgNsfwScan: NsfwScan? = null
        if (avgNsfwScan != null) {
            avgNsfwScan.media = savedMedia
            savedAvgNsfwScan = nsfwScanRepository.save(avgNsfwScan)
        }

        var savedMaxNsfwScan: NsfwScan? = null
        if (maxNsfwScan != null) {
            maxNsfwScan.media = savedMedia
            savedMaxNsfwScan = nsfwScanRepository.save(maxNsfwScan)
        }

        return MediaUploadResult(savedMedia, savedAvgNsfwScan, savedMaxNsfwScan)
    }

    @Transactional
    fun saveFile(fileUploadDto: FileUploadDto): File {
        val file = File(fileUploadDto.name, fileUploadDto.id, fileUploadDto.threat)

        return fileRepository.saveAndFlush(file)
    }

    @Transactional
    fun saveSquareImage(squareImageSaveDto: SquareImageSaveDto, user: User): SquareImageSaveResult {
        val media = Media(
                name = UUID.fromString(squareImageSaveDto.id),
                duration = null,
                phash = null,
                mediaType = MediaType.IMAGE,
                user
        )

        val savedMedia = mediaRepository.saveAndFlush(media)
        val nsfwScan = createNsfwScan(squareImageSaveDto.avgNsfw, NsfwScanType.AVERAGE)
        nsfwScan.media = savedMedia
        val savedNsfwScan = nsfwScanRepository.save(nsfwScan)

        return SquareImageSaveResult(savedMedia, savedNsfwScan)
    }

    private fun createNsfwScan(nsfwScanDto: NsfwScanDto, nsfwScanType: NsfwScanType): NsfwScan {
        return NsfwScan(
                drawings = nsfwScanDto.drawings,
                hentai = nsfwScanDto.hentai,
                neutral = nsfwScanDto.neutral,
                porn = nsfwScanDto.porn,
                sexy = nsfwScanDto.sexy,
                type = nsfwScanType,
                media = null
        )
    }

    fun uploadMediaToServer(file: MultipartFile,
                            shouldHash: Boolean = true,
                            shouldNsfwScan: Boolean = true): MediaSaveDto? {
        val builder = MultipartBodyBuilder()

        val header = String.format("form-data; name=%s; filename=%s", "file", file.originalFilename)
        builder.part("file", ByteArrayResource(file.bytes)).header("Content-Disposition", header)

        val content = webClient.post()
                .uri {
                    it.path("$mediaServerUrl/media")
                            .queryParam("phash", shouldHash)
                            .queryParam("nsfw", shouldNsfwScan)
                            .build()
                }
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError) {
                    return@onStatus Mono.empty()
                }
                .bodyToMono(MediaSaveDto::class.java)
                .blockOptional()

        if (content.isEmpty) {
            return null
        }

        return content.get()
    }

    fun getMediaFromServer(name: String): MediaResponse? {
        val content: Optional<ResponseEntity<DataBuffer>> = webClient.get()
                .uri("$mediaServerUrl/media/$name")
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError) {
                    return@onStatus Mono.empty()
                }
                .toEntity(DataBuffer::class.java)
                .blockOptional()

        if (content.isEmpty || content.get().body == null || content.get().headers.contentType == null) {
            return null
        }

        return MediaResponse(content.get().body!!.asByteBuffer().array(),
                content.get().headers.contentType!!)
    }

    fun deleteMediaFromServer(name: String): Boolean {
        val content = webClient.delete()
                .uri("$mediaServerUrl/media/$name")
                .exchangeToMono {
                    if (it.statusCode() == HttpStatus.OK) {
                        return@exchangeToMono Mono.just(true)
                    } else {
                        return@exchangeToMono Mono.just(false)
                    }
                }
                .blockOptional()

        return !content.isEmpty
    }

    fun uploadFileToServer(file: MultipartFile): FileUploadDto? {
        val builder = MultipartBodyBuilder()

        val header = String.format("form-data; name=%s; filename=%s", "file", file.originalFilename)
        builder.part("file", ByteArrayResource(file.bytes)).header("Content-Disposition", header)

        val content = webClient.post()
                .uri("$mediaServerUrl/file")
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError) {
                    return@onStatus Mono.empty()
                }
                .bodyToMono(FileUploadDto::class.java)
                .blockOptional()

        if (content.isEmpty) {
            return null
        }

        return content.get()
    }

    fun getFileFromServer(name: String): ByteArray? {
        val content = webClient.get()
                .uri("$mediaServerUrl/file/$name")
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError) {
                    return@onStatus Mono.empty()
                }
                .bodyToMono(DataBuffer::class.java)
                .blockOptional()

        if (content.isEmpty) {
            return null
        }

        return content.get().asByteBuffer().array()
    }

    fun deleteFileFromServer(name: String): Boolean {
        val content = webClient.delete()
                .uri("$mediaServerUrl/file/$name")
                .exchangeToMono {
                    if (it.statusCode() == HttpStatus.OK) {
                        return@exchangeToMono Mono.just(true)
                    } else {
                        return@exchangeToMono Mono.just(false)
                    }
                }
                .blockOptional()

        return !content.isEmpty
    }

    fun uploadSquareImageToServer(file: MultipartFile, size: Int, left: Int, top: Int, cropSize: Int): SquareImageSaveDto? {
        val builder = MultipartBodyBuilder()

        val header = String.format("form-data; name=%s; filename=%s", "file", file.originalFilename)
        builder.part("file", ByteArrayResource(file.bytes)).header("Content-Disposition", header)

        val content = webClient.post()
                .uri {
                    it.path("$mediaServerUrl/image/square")
                            .queryParam("size", size)
                            .queryParam("left", left)
                            .queryParam("top", top)
                            .queryParam("cropSize", cropSize)
                            .build()
                }
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError) {
                    return@onStatus Mono.empty()
                }
                .bodyToMono(SquareImageSaveDto::class.java)
                .blockOptional()

        if (content.isEmpty) {
            return null
        }

        return content.get()
    }

    fun inNsfwRequirements(nsfwScan: NsfwScan): Boolean {
        return nsfwScan.drawings <= Constants.MAX_DRAWING_TOLERANCE &&
                nsfwScan.hentai <= Constants.MAX_HENTAI_TOLERANCE &&
                nsfwScan.neutral <= Constants.MAX_NEUTRAL_TOLERANCE &&
                nsfwScan.sexy <= Constants.MAX_SEXY_TOLERANCE &&
                nsfwScan.porn <= Constants.MAX_PORN_TOLERANCE
    }
}