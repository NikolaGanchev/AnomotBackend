package com.anomot.anomotbackend

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper

class TestUtils {
    companion object {
        fun objectToJson(obj: Any): ByteArray {
            val mapper = ObjectMapper()
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            return mapper.writeValueAsBytes(obj)
        }

        fun objectToJsonString(obj: Any): String {
            val mapper = ObjectMapper()
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            return mapper.writeValueAsString(obj)
        }

    }
}