package com.apptmgmt.communication

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.apptmgmt.communication.data.CommunicationApi
import com.apptmgmt.communication.domain.ChatInputValidator
import com.apptmgmt.communication.domain.ChatMessage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val api by lazy { CommunicationApi(BuildConfig.API_BASE_URL) }
    private lateinit var session: SessionViewModel
    private var statusText: TextView? = null
    private var messageList: LinearLayout? = null
    private var sendButton: Button? = null
    private var messageInput: EditText? = null
    private var sending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = ViewModelProvider(this)[SessionViewModel::class.java]
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.rgb(18, 79, 69)
        if (session.token == null) {
            showSignIn()
        } else {
            showChat()
            refreshMessages()
        }
    }

    private fun showSignIn(error: String? = null) {
        session.token = null
        val root = createRoot()
        root.addView(text(getString(R.string.login_title), 26, Color.rgb(18, 79, 69), true))
        root.addView(text(getString(R.string.login_subtitle), 16, Color.DKGRAY))

        val emailInput = field(getString(R.string.email_label), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val passwordInput = field(getString(R.string.password_label), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        root.addView(emailInput, fieldParams())
        root.addView(passwordInput, fieldParams())

        val status = text(error.orEmpty(), 14, Color.rgb(166, 42, 42))
        root.addView(status, fieldParams())
        statusText = status

        val signInButton = button(getString(R.string.sign_in))
        root.addView(signInButton, fieldParams())
        signInButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            when {
                !ChatInputValidator.isValidEmail(email) -> status.text = getString(R.string.invalid_email)
                !ChatInputValidator.isValidPassword(password) -> status.text = getString(R.string.missing_password)
                else -> {
                    signInButton.isEnabled = false
                    status.text = getString(R.string.loading)
                    lifecycleScope.launch {
                        try {
                            session.token = api.signIn(email, password)
                            showChat()
                            refreshMessages()
                        } catch (error: CommunicationApi.ApiException) {
                            status.text = error.message ?: getString(R.string.server_error, error.statusCode)
                        } catch (_: java.net.SocketTimeoutException) {
                            status.text = getString(R.string.network_error)
                        } catch (_: java.net.UnknownHostException) {
                            status.text = getString(R.string.network_error)
                        } catch (error: java.io.IOException) {
                            status.text = error.localizedMessage ?: getString(R.string.network_error)
                        } finally {
                            signInButton.isEnabled = true
                        }
                    }
                }
            }
        }

        root.addView(text(getString(R.string.medical_disclaimer), 13, Color.GRAY), fieldParams())
        display(root)
    }

    private fun showChat() {
        val root = createRoot()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = text(getString(R.string.chat_title), 24, Color.rgb(18, 79, 69), true)
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(button(getString(R.string.sign_out)).apply {
            setOnClickListener {
                session.token = null
                session.messages = emptyList()
                showSignIn()
            }
        })
        root.addView(header)
        root.addView(text(getString(R.string.medical_disclaimer), 13, Color.GRAY), fieldParams())

        val status = text("", 14, Color.DKGRAY)
        root.addView(status, fieldParams())
        statusText = status

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
        }
        val messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        scrollView.addView(messages, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(scrollView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        messageList = messages
        renderMessages()

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            hint = getString(R.string.message_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 1
            maxLines = 4
        }
        messageInput = input
        composer.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val send = button(getString(R.string.send))
        sendButton = send
        composer.addView(send)
        root.addView(composer, fieldParams())
        send.setOnClickListener { sendMessage() }

        val refresh = button(getString(R.string.refresh))
        root.addView(refresh, fieldParams())
        refresh.setOnClickListener { refreshMessages() }
        display(root)
    }

    private fun refreshMessages() {
        val token = session.token ?: return
        statusText?.text = getString(R.string.loading)
        lifecycleScope.launch {
            try {
                session.messages = api.getMessages(token)
                renderMessages()
                statusText?.text = if (session.messages.isEmpty()) getString(R.string.empty_messages) else ""
            } catch (error: CommunicationApi.ApiException) {
                handleApiError(error)
            } catch (_: java.net.SocketTimeoutException) {
                statusText?.text = getString(R.string.network_error)
            } catch (_: java.net.UnknownHostException) {
                statusText?.text = getString(R.string.network_error)
            } catch (error: java.io.IOException) {
                statusText?.text = error.localizedMessage ?: getString(R.string.network_error)
            }
        }
    }

    private fun sendMessage() {
        if (sending) return
        val message = ChatInputValidator.normalizedMessage(messageInput?.text?.toString().orEmpty())
        if (message == null) {
            statusText?.text = getString(R.string.empty_message)
            return
        }
        val token = session.token ?: return
        sending = true
        sendButton?.isEnabled = false
        statusText?.text = getString(R.string.sending)
        lifecycleScope.launch {
            try {
                api.sendMessage(token, message)
                messageInput?.text?.clear()
                refreshMessages()
            } catch (error: CommunicationApi.ApiException) {
                handleApiError(error)
            } catch (_: java.net.SocketTimeoutException) {
                statusText?.text = getString(R.string.network_error)
            } catch (_: java.net.UnknownHostException) {
                statusText?.text = getString(R.string.network_error)
            } catch (error: java.io.IOException) {
                statusText?.text = error.localizedMessage ?: getString(R.string.network_error)
            } finally {
                sending = false
                sendButton?.isEnabled = true
            }
        }
    }

    private fun handleApiError(error: CommunicationApi.ApiException) {
        if (error.statusCode == 401 || error.statusCode == 403) {
            showSignIn(getString(R.string.invalid_session))
        } else {
            statusText?.text = error.message ?: getString(R.string.server_error, error.statusCode)
        }
    }

    private fun renderMessages() {
        val container = messageList ?: return
        container.removeAllViews()
        session.messages.forEach { message ->
            val isPatient = message.sender == ChatMessage.Sender.PATIENT
            val messageBlock = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = if (isPatient) Gravity.END else Gravity.START
                setPadding(0, dp(4), 0, dp(4))
            }
            val bubble = text(message.text, 16, if (isPatient) Color.WHITE else Color.rgb(32, 45, 43))
            bubble.background = GradientDrawable().apply {
                setColor(if (isPatient) Color.rgb(23, 107, 91) else Color.rgb(235, 241, 239))
                cornerRadius = dp(16).toFloat()
            }
            bubble.setPadding(dp(14), dp(10), dp(14), dp(10))
            messageBlock.addView(bubble, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (isPatient) marginStart = dp(48) else marginEnd = dp(48)
            })
            messageBlock.addView(text(
                "${getString(if (isPatient) R.string.patient_sender else R.string.doctor_sender)} · ${formatTimestamp(message.timestamp)}",
                12,
                Color.GRAY
            ))
            container.addView(messageBlock)
        }
    }

    private fun formatTimestamp(value: String): String {
        return value.replace('T', ' ')
            .removeSuffix("Z")
            .substringBefore('.')
            .take(16)
    }

    private fun createRoot() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(24), dp(20), dp(16))
        setBackgroundColor(Color.WHITE)
    }

    private fun display(root: LinearLayout) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val safeInsets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val keyboardInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                dp(20) + safeInsets.left,
                dp(24) + safeInsets.top,
                dp(20) + safeInsets.right,
                dp(16) + maxOf(safeInsets.bottom, keyboardInsets.bottom)
            )
            windowInsets
        }
        setContentView(root)
    }

    private fun field(hintText: String, type: Int) = EditText(this).apply {
        hint = hintText
        inputType = type
        singleLine = true
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun button(label: String) = Button(this).apply {
        text = label
        isAllCaps = false
    }

    private fun fieldParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        topMargin = dp(12)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
