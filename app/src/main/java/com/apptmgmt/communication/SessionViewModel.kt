package com.apptmgmt.communication

import androidx.lifecycle.ViewModel
import com.apptmgmt.communication.domain.ChatMessage

class SessionViewModel : ViewModel() {
    var token: String? = null
    var messages: List<ChatMessage> = emptyList()
}
