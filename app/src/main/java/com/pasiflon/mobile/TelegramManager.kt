package com.pasiflon.mobile

import android.content.Context
import android.util.Log
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File

object TelegramManager {
    private var client: Client? = null
    private var authState: TdApi.AuthorizationState? = null
    private var apiId: Int = 0
    private var apiHash: String = ""

    interface AuthListener {
        fun onNeedPhone()
        fun onNeedCode()
        fun onLoginSuccess()
        fun onNewMessage(msg: TelegramMsg) // הוספנו: עדכון על הודעה חדשה
        fun onError(msg: String)
    }
    
    var listener: AuthListener? = null

    fun initClient(context: Context, strApiId: String, strApiHash: String) {
        if (client != null) return
        
        try {
            apiId = strApiId.toInt()
            apiHash = strApiHash
        } catch (e: Exception) { return }

        // יצירת הלקוח עם טיפול בעדכונים
        client = Client.create(
            { update ->
                when (update) {
                    is TdApi.UpdateAuthorizationState -> {
                        authState = update.authorizationState
                        handleAuthState(context)
                    }
                    is TdApi.UpdateNewMessage -> {
                        // הנה הלב של הפיד: הודעה חדשה נכנסה!
                        handleNewMessage(update.message)
                    }
                }
            },
            { e -> listener?.onError("TDLib Error: ${e.localizedMessage}") },
            { e -> listener?.onError("TDLib Exception: ${e.localizedMessage}") }
        )
    }

    private fun handleNewMessage(message: TdApi.Message) {
        // אנחנו מסננים הודעות יוצאות (שלנו) ומתמקדמים בנכנסות
        // if (message.isOutgoing) return 

        var textContent = ""
        var hasMedia = false
        var imagePath: String? = null

        when (val content = message.content) {
            is TdApi.MessageText -> {
                textContent = content.text.text
            }
            is TdApi.MessageVideo -> {
                hasMedia = true
                textContent = content.caption.text
                // כאן בעתיד נחלץ את ה-Thumbnail
            }
            is TdApi.MessagePhoto -> {
                hasMedia = true
                textContent = content.caption.text
            }
        }

        // אם יש תוכן, שולחים למסך הראשי
        if (textContent.isNotEmpty() || hasMedia) {
            // שליפת שם השולח דורשת קריאה נפרדת (GetUser/GetChat), כרגע נציג ID למהירות
            val senderName = "Chat ${message.chatId}" 
            
            val uiMsg = TelegramMsg(
                id = message.id,
                sender = senderName,
                text = textContent.ifEmpty { "מדיה ללא כיתוב" },
                hasMedia = hasMedia
            )
            listener?.onNewMessage(uiMsg)
        }
    }

    private fun handleAuthState(context: Context) {
        when (authState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val params = TdApi.TdlibParameters()
                params.databaseDirectory = File(context.filesDir, "tdlib").absolutePath
                params.useMessageDatabase = true
                params.useSecretChats = true
                params.apiId = apiId
                params.apiHash = apiHash
                params.systemLanguageCode = "en"
                params.deviceModel = "Pasiflon Cyber"
                params.applicationVersion = "1.0"
                client?.send(TdApi.SetTdlibParameters(params)) { }
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> listener?.onNeedPhone()
            is TdApi.AuthorizationStateWaitCode -> listener?.onNeedCode()
            is TdApi.AuthorizationStateReady -> listener?.onLoginSuccess()
        }
    }

    fun sendPhoneNumber(phone: String) {
        client?.send(TdApi.SetAuthenticationPhoneNumber(phone, TdApi.PhoneNumberAuthenticationSettings())) {}
    }

    fun sendCode(code: String) {
        client?.send(TdApi.CheckAuthenticationCode(code)) {}
    }
}
