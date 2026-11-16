package com.apptmgmt.communication.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatInputValidatorTest {
    @Test
    fun acceptsValidEmailAndNonBlankPassword() {
        assertTrue(ChatInputValidator.isValidEmail(" patient@example.com "))
        assertTrue(ChatInputValidator.isValidPassword("secret"))
    }

    @Test
    fun rejectsMalformedEmailAndBlankPassword() {
        assertFalse(ChatInputValidator.isValidEmail("patient.example.com"))
        assertFalse(ChatInputValidator.isValidPassword("  "))
    }

    @Test
    fun messageIsTrimmedAndBlankMessageIsRejected() {
        assertEquals("Necesito ayuda", ChatInputValidator.normalizedMessage("  Necesito ayuda  "))
        assertEquals(null, ChatInputValidator.normalizedMessage(" \n "))
    }
}
