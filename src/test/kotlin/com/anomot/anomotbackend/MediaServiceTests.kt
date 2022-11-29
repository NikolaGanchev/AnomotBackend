package com.anomot.anomotbackend

import com.anomot.anomotbackend.dto.NsfwScanDto
import com.anomot.anomotbackend.repositories.FileRepository
import com.anomot.anomotbackend.repositories.MediaRepository
import com.anomot.anomotbackend.repositories.NsfwScanRepository
import com.anomot.anomotbackend.services.MediaService
import com.anomot.anomotbackend.utils.MediaType
import com.anomot.anomotbackend.utils.NsfwScanType
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.WebClient
import java.util.HexFormat
import java.util.UUID

@SpringBootTest
class MediaServiceTests @Autowired constructor(
        @InjectMockKs
        private val mediaService: MediaService
) {
    @Autowired
    private lateinit var webClient: WebClient
    @MockkBean
    private lateinit var mediaRepository: MediaRepository
    @MockkBean
    private lateinit var nsfwScanRepository: NsfwScanRepository
    @MockkBean
    private lateinit var fileRepository: FileRepository
    private lateinit var mockWebServer: MockWebServer

    private final val mockMediaResponseFull = object {
        val type = "video"
        val phash = "c1951669b31e60fb"
        val id = "d22ffc47-78b3-4d90-be1c-424908324d90"
        val avgNsfw = NsfwScanDto(
                0.008960937149822712f,
                0.016827013343572617f,
                0.9502359628677368f,
                0.01023074984550476f,
                0.013745378702878952f
        )
        val maxNsfw = NsfwScanDto(
                0.008960937149822712f,
                0.016827013343572617f,
                0.9502359628677368f,
                0.01023074984550476f,
                0.013745378702878952f
        )
        val duration = 24.0
    }

    @BeforeAll
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start(8000)
        ReflectionTestUtils.setField(mediaService, "mediaServerUrl", "localhost:8000")
    }

    @AfterAll
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `When upload media then return correct response`() {
        mockWebServer.enqueue(MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(TestUtils.objectToJsonString(mockMediaResponseFull)))

        every { mediaRepository.saveAndFlush(any()) } returnsArgument 0
        every { nsfwScanRepository.save(any()) } returnsArgument 0

        val result = mediaService.uploadMedia(MockMultipartFile("hello.png",
                "hello.png",
                "image/png",
                null))

        assertThat(result).isNotNull
        assertThat(result!!.media).isNotNull
        assertThat(result.avgNsfwScan).isNotNull
        assertThat(result.maxNsfwScan).isNotNull

        val media = result.media
        val avgNsfwScan = result.avgNsfwScan
        val maxNsfwScan = result.maxNsfwScan

        assertThat(media!!.name).isEqualTo(UUID.fromString(mockMediaResponseFull.id))
        assertThat(media.mediaType).isEqualTo(MediaType.VIDEO)
        assertThat(media.phash).isEqualTo(HexFormat.of().parseHex(mockMediaResponseFull.phash))
        assertThat(media.duration).isEqualTo(24f)
        assertThat(avgNsfwScan!!.drawings).isEqualTo(0.008960937f)
        assertThat(avgNsfwScan.type).isEqualTo(NsfwScanType.AVERAGE)
        assertThat(maxNsfwScan!!.neutral).isEqualTo(0.95023596f)
        assertThat(maxNsfwScan.type).isEqualTo(NsfwScanType.MAX)
    }
}