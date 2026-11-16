package com.apptmgmt.communication.domain

object ChatInputValidator {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValidEmail(email: String): Boolean = emailPattern.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.isNotBlank()

    fun normalizedMessage(message: String): String? =
        message.trim().takeIf(String::isNotEmpty)
}
