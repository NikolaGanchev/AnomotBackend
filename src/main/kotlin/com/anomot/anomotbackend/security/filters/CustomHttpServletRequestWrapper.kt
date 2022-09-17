package com.anomot.anomotbackend.security.filters

import java.util.*
import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

// This class is needed to be able to set parameters on request (used for converting form encoding to json)

class CustomHttpServletRequestWrapper(request: ServletRequest) : HttpServletRequestWrapper(request as HttpServletRequest?) {
    private val params: MutableMap<String, Array<String>> = mutableMapOf()

    fun setParameter(name: String, value: String) {
        params[name] = arrayOf(value)
    }

    override fun getParameter(name: String): String? {
        val values = parameterMap[name] ?: return null
        return Arrays.stream(values)
                .findFirst()
                .orElse(super.getParameter(name))
    }

    override fun getParameterMap(): Map<String, Array<String>> {
        val parameters =  mutableMapOf<String, Array<String>>()
        parameters.putAll(super.getParameterMap())
        parameters.putAll(params)
        return Collections.unmodifiableMap(parameters)
    }

    override fun getParameterNames(): Enumeration<String?>? {
        return Collections.enumeration(parameterMap.keys)
    }

    override fun getParameterValues(name: String): Array<String>? {
        return parameterMap[name]
    }

}