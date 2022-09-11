package com.anomot.anomotbackend.security.filters

import com.anomot.anomotbackend.utils.Constants
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class CustomJsonReaderFilter:
        OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (request.contentType != "application/json" || !request.requestURL.endsWith("account/login")) {
            chain.doFilter(request, response)
        } else {

            val requestMap: MutableMap<String, String> = ObjectMapper()
                    .readValue(request.inputStream, MutableMap::class.java)
                    as? MutableMap<String, String>
                    ?: throw AuthenticationServiceException("Could not read message")

            val email = requestMap[Constants.USERNAME_PARAMETER]
                    ?: throw AuthenticationServiceException("Could not read argument")

            val password = requestMap[Constants.PASSWORD_PARAMETER]
                    ?: throw AuthenticationServiceException("Could not read argument")

            val mutableRequestWrapper = CustomHttpServletRequestWrapper(request)

            mutableRequestWrapper.setParameter(Constants.USERNAME_PARAMETER, email)
            mutableRequestWrapper.setParameter(Constants.PASSWORD_PARAMETER, password)

            chain.doFilter(mutableRequestWrapper, response)
        }
    }
}