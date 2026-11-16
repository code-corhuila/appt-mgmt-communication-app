package com.apptmgmt.communication.data

import com.apptmgmt.communication.domain.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class CommunicationApi(baseUrl: String) {
    private val normalizedBaseUrl = baseUrl.trimEnd('/') + "/"

    init {
        val endpoint = URI(normalizedBaseUrl)
        require(
            endpoint.scheme.equals("https", ignoreCase = true) &&
                !endpoint.host.isNullOrBlank() &&
                endpoint.rawUserInfo == null &&
                endpoint.rawQuery == null &&
                endpoint.rawFragment == null
        ) {
            "The communication API must use an HTTPS base URL."
        }
    }

    suspend fun signIn(email: String, password: String): String = withContext(Dispatchers.IO) {
        val response = request(
            method = "POST",
            path = "api/auth/login",
            body = JSONObject()
                .put("email", email.trim())
                .put("contrasena", password)
                .toString()
        )
        try {
            JSONObject(response).optString("token").takeIf(String::isNotBlank)
                ?: throw ApiException(502, "The sign-in response did not contain a session token.")
        } catch (error: JSONException) {
            throw ApiException(502, "The sign-in response was not valid JSON.", error)
        }
    }

    suspend fun getMessages(token: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val response = request("GET", "api/chat", token = token)
        try {
            val array = JSONArray(response)
            List(array.length()) { index -> parseMessage(array.getJSONObject(index)) }
        } catch (error: JSONException) {
            throw ApiException(502, "The message history response was invalid.", error)
        }
    }

    suspend fun sendMessage(token: String, text: String): ChatMessage = withContext(Dispatchers.IO) {
        val response = request(
            method = "POST",
            path = "api/chat",
            token = token,
            body = JSONObject().put("texto", text).toString()
        )
        try {
            parseMessage(JSONObject(response))
        } catch (error: JSONException) {
            throw ApiException(502, "The sent-message response was invalid.", error)
        }
    }

    private fun parseMessage(json: JSONObject): ChatMessage {
        val id = json.optString("_id").ifBlank { json.optString("id") }
        val sender = when (json.optString("remitente")) {
            "PACIENTE" -> ChatMessage.Sender.PATIENT
            "DOCTOR" -> ChatMessage.Sender.CARE_TEAM
            else -> throw ApiException(502, "A message had an unknown sender.")
        }
        val text = json.optString("texto")
        val timestamp = json.optString("fecha")
        if (id.isBlank() || text.isBlank() || timestamp.isBlank()) {
            throw ApiException(502, "A message was missing required fields.")
        }
        return ChatMessage(id = id, sender = sender, text = text, timestamp = timestamp)
    }

    private fun request(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null
    ): String {
        val connection = URL(normalizedBaseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 90_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-store")
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw ApiException(status, readErrorMessage(responseBody, status))
            }
            return responseBody
        } finally {
            connection.disconnect()
        }
    }

    private fun readErrorMessage(body: String, status: Int): String {
        if (body.isNotBlank()) {
            try {
                JSONObject(body).optString("mensaje").takeIf(String::isNotBlank)?.let { return it }
            } catch (_: JSONException) {
                return "The communication API returned HTTP $status."
            }
        }
        return "The communication API returned HTTP $status."
    }

    class ApiException(
        val statusCode: Int,
        message: String,
        cause: Throwable? = null
    ) : IOException(message, cause)
}
