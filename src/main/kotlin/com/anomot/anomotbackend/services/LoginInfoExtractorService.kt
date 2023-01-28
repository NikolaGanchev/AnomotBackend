package com.anomot.anomotbackend.services

import com.anomot.anomotbackend.AnomotBackendApplication
import com.anomot.anomotbackend.dto.LoginInfoDto
import com.anomot.anomotbackend.entities.SuccessfulLogin
import com.anomot.anomotbackend.entities.User
import com.anomot.anomotbackend.repositories.SuccessfulLoginRepository
import com.anomot.anomotbackend.repositories.UserRepository
import com.anomot.anomotbackend.security.CustomUserDetails
import com.blueconic.browscap.UserAgentParser
import com.blueconic.browscap.UserAgentService
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.exception.AddressNotFoundException
import com.maxmind.geoip2.model.CityResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.net.InetAddress
import java.util.*


@Service
class LoginInfoExtractorService @Autowired constructor(
        private val successfulLoginRepository: SuccessfulLoginRepository,
        private val userRepository: UserRepository,
        private val notificationService: NotificationService
) {
    private var dbReader: DatabaseReader? = null
    private val userAgentParser: UserAgentParser
    private val logger: Logger = LoggerFactory.getLogger(AnomotBackendApplication::class.java)
    @Value("\${environment.is-local}")
    private val isLocal: String? = null

    init {
        dbReader = try {
            val db = ClassPathResource("/GeoLite2/GeoLite2-City.mmdb").inputStream
            DatabaseReader.Builder(db).build()
        } catch (exception: FileNotFoundException) {
            if (isLocal != null && isLocal.toBoolean()) {
                throw exception
            }
            null
        }
        userAgentParser = UserAgentService().loadParser()
    }

    fun getInfo(ip: String, userAgent: String?): SuccessfulLogin {
        val address = InetAddress.getByName(ip)
        var response: CityResponse? = null

        try {
            response = dbReader?.city(address)
        } catch(exception: AddressNotFoundException) {
            logger.error("Ip not found $ip")
        }

        var cityName = "Unknown"
        var countryName= "Unknown"

        if (response != null) {
            cityName = response.city.name
            countryName = response.country.name
        }

        if (userAgent == null) {
            return SuccessfulLogin(
                    city = cityName,
                    country = countryName,
                    deviceType = null,
                    platform = null,
                    platformVersion = null,
                    browser = null,
                    browserVersion = null)
        }

        val capabilities = userAgentParser.parse(userAgent)

        return SuccessfulLogin(
                city = cityName,
                country = countryName,
                deviceType = capabilities.deviceType,
                platform = capabilities.platform,
                platformVersion = capabilities.platformVersion,
                browser = capabilities.browser,
                browserVersion = capabilities.browserMajorVersion)
    }

    fun saveLoginAndSendNotification(userDetails: CustomUserDetails, successfulLogin: SuccessfulLogin) {
        val user = userRepository.getReferenceById(userDetails.id!!)
        successfulLogin.user = user
        successfulLogin.date = Date()
        val saved = successfulLoginRepository.save(successfulLogin)
        notificationService.sendNewLoginNotification(user, saved)
    }

    fun getByUser(userDetails: CustomUserDetails, pageRequest: PageRequest): List<LoginInfoDto> {
        val user = userRepository.getReferenceById(userDetails.id!!)

        return successfulLoginRepository.findAllByUser(user, pageRequest).map(LoginInfoDto.Companion::from)
    }

    fun getByUser(user: User, pageRequest: PageRequest): List<LoginInfoDto> {
        return successfulLoginRepository.findAllByUser(user, pageRequest).map(LoginInfoDto.Companion::from)
    }

    fun getByUserAndId(userDetails: CustomUserDetails, id: String): LoginInfoDto? {
        val user = userRepository.getReferenceById(userDetails.id!!)
        val idLong = try {
            id.toLong()
        } catch (e: NumberFormatException) {
            return null
        }

        val result = successfulLoginRepository.findByUserAndId(user, idLong) ?: return null

        return LoginInfoDto.from(result)
    }

    fun getLoginReferenceFromIdUnsafe(id: String): SuccessfulLogin? {
        val loginId = id.toLongOrNull() ?: return null

        if (!successfulLoginRepository.existsById(loginId)) return null

        return successfulLoginRepository.getReferenceById(loginId)
    }
}