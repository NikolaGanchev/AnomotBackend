package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.AnomotBackendApplication
import com.anomot.anomotbackend.dto.*
import com.anomot.anomotbackend.entities.*
import com.anomot.anomotbackend.repositories.*
import com.anomot.anomotbackend.utils.Constants
import com.anomot.anomotbackend.utils.MediaType
import com.anomot.anomotbackend.utils.NsfwScanType
import com.anomot.anomotbackend.utils.SecureRandomStringGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.lang.Exception
import java.util.*
import javax.transaction.Transactional


@Service
class MediaService @Autowired constructor(
        private val webClient: WebClient,
        private val mediaRepository: MediaRepository,
        private val nsfwScanRepository: NsfwScanRepository,
        private val fileRepository: FileRepository,
        private val urlRepository: UrlRepository,
        private val appealRepository: AppealRepository,
        @Value("\${environment.media_server_url}")
        private val mediaServerUrl: String
) {
    data class MediaUploadResult(val media: Media?, val avgNsfwScan: NsfwScan?, val maxNsfwScan: NsfwScan?)
    data class SquareImageSaveResult(val media: Media?, val avgNsfwScan: NsfwScan?)
    private val logger: Logger = LoggerFactory.getLogger(AnomotBackendApplication::class.java)

    val secureRandomStringGenerator = SecureRandomStringGenerator(SecureRandomStringGenerator.ALPHANUMERIC)

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
    fun saveFile(fileUploadDto: FileUploadDto, user: User): File {
        val file = File(fileUploadDto.name, UUID.fromString(fileUploadDto.id), fileUploadDto.threat, user)

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

        try {
            val content = webClient.post()
                    .uri {
                        it.path("$mediaServerUrl/media")
                                .queryParam("phash", shouldHash)
                                .queryParam("nsfw", shouldNsfwScan)
                                .build()
                    }
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .accept(org.springframework.http.MediaType.APPLICATION_JSON, org.springframework.http.MediaType.TEXT_PLAIN)
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
        } catch (e: Exception) {
            logger.error(e.message)
            return null
        }
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

    fun massDeleteMediaFromServer(names: List<String>): Boolean {
        val content = webClient.method(HttpMethod.DELETE)
                .uri("$mediaServerUrl/media/mass")
                .bodyValue(object {
                    val ids = names
                })
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
                .accept(org.springframework.http.MediaType.APPLICATION_JSON, org.springframework.http.MediaType.TEXT_PLAIN)
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

    fun massDeleteFilesFromServer(names: List<String>): Boolean {
        val content = webClient.method(HttpMethod.DELETE)
                .uri("$mediaServerUrl/file/mass")
                .bodyValue(object {
                    val ids = names
                })
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

        try {
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
                    .onErrorResume(WebClientResponseException.UnsupportedMediaType::class.java) {
                        return@onErrorResume Mono.empty()
                    }
                    .blockOptional()

            if (content.isEmpty) {
                return null
            }
            return content.get()
        } catch (e: Exception) {
            logger.error(e.message)
            return null
        }
    }

    fun inNsfwRequirements(media: MediaResponse, name: String): Boolean {
        val nsfwScan = nsfwScanRepository.getMaxAndAverageByMediaName(UUID.fromString(name))
        val nsfwStats = nsfwScan.maxNsfwScans ?: nsfwScan.avgNsfwScan!!

        return inNsfwRequirements(nsfwStats)
    }

    fun inNsfwRequirements(nsfwScan: NsfwScan): Boolean {
        return nsfwScan.drawings <= Constants.MAX_DRAWING_TOLERANCE &&
                nsfwScan.hentai <= Constants.MAX_HENTAI_TOLERANCE &&
                nsfwScan.neutral <= Constants.MAX_NEUTRAL_TOLERANCE &&
                nsfwScan.sexy <= Constants.MAX_SEXY_TOLERANCE &&
                nsfwScan.porn <= Constants.MAX_PORN_TOLERANCE
    }

    fun getMediaByUser(user: User, page: Int, pageSize: Int = Constants.MEDIA_PAGE): List<Media> {
        return mediaRepository.getMediaByPublisher(user, PageRequest.of(page, pageSize, Sort.by("creationDate").descending()))
    }

    fun getFilesByUser(user: User, page: Int, pageSize: Int = Constants.MEDIA_PAGE): List<File> {
        return fileRepository.getFilesByUploader(user, PageRequest.of(page, pageSize, Sort.by("creationDate").descending()))
    }

    fun uploadUrl(url: String, user: User): String {
        val newUrl = urlRepository.save(Url(url, secureRandomStringGenerator.generate(Constants.URL_LENGTH), user))
        return newUrl.inAppUrl
    }

    fun getRealUrl(inAppUrl: String): String? {
        val newUrl = urlRepository.getByInAppUrl(inAppUrl)
        return newUrl?.url
    }

    @Transactional
    fun deleteMedia(media: Media): Boolean {
        appealRepository.deleteByMedia(media)
        nsfwScanRepository.deleteByMedia(media)
        mediaRepository.delete(media)

        return deleteMediaFromServer(media.name.toString())
    }

    fun deleteMediaByUserWithoutNsfwScans(user: User) {
        var page = 0
        do {
            val result = getMediaByUser(user, page, 20)
            massDeleteMediaFromServer(result.map {
                it.name.toString()
            })
            page++
        } while (result.isNotEmpty())

        mediaRepository.deleteByUser(user)
    }

    fun deleteFilesByUser(user: User) {
        var page = 0
        do {
            val result = getFilesByUser(user, page, 20)
            massDeleteFilesFromServer(result.map {
                it.name.toString()
            })
            page++
        } while (result.isNotEmpty())

        fileRepository.deleteByUser(user)
    }
}