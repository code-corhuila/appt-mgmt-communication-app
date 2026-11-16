package com.apptmgmt.communication.data

import org.junit.Assert.assertThrows
import org.junit.Test

class CommunicationApiTest {
    @Test
    fun rejectsNonHttpsApiEndpoints() {
        assertThrows(IllegalArgumentException::class.java) {
            CommunicationApi("http://api.example/")
        }
    }

    @Test
    fun rejectsCredentialsEmbeddedInApiEndpoint() {
        assertThrows(IllegalArgumentException::class.java) {
            CommunicationApi("https://user:password@api.example/")
        }
    }
}
