package com.apptmgmt.communication.domain

data class ChatMessage(
    val id: String,
    val sender: Sender,
    val text: String,
    val timestamp: String
) {
    enum class Sender {
        PATIENT,
        CARE_TEAM
    }
}
