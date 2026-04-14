package com.withcall.app

import android.content.Context
import android.telephony.SmsManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

class SmsProcessor {

    private val inbox = Channel<Pair<String, String>>(Channel.BUFFERED)

    companion object {
        var instance: SmsProcessor? = null

        fun normalizeNumber(number: String): String {
            var n = number.trim().replace(" ", "").replace("-", "")
            if (n.startsWith("+82")) n = "0" + n.substring(3)
            return n
        }

        fun sendSms(context: Context, phoneNumber: String, message: String) {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
        }
    }

    fun onSmsReceived(sender: String, message: String) {
        inbox.trySend(Pair(sender, message))
    }

    /**
     * fromNumber로부터 SMS를 최대 timeoutMs 밀리초 대기.
     * - expectedTexts 미지정 → 해당 번호의 모든 메시지 수락
     * - expectedTexts 지정  → 메시지가 그 중 하나라도 포함하면 수락
     */
    suspend fun waitForResponse(
        fromNumber: String,
        timeoutMs: Long,
        vararg expectedTexts: String
    ): String? {
        val target = normalizeNumber(fromNumber)
        return withTimeoutOrNull(timeoutMs) {
            var result: String? = null
            while (result == null) {
                val (sender, message) = inbox.receive()
                if (normalizeNumber(sender) == target) {
                    val matched = expectedTexts.isEmpty() ||
                                  expectedTexts.any { message.contains(it) }
                    if (matched) result = message.trim()
                }
            }
            result
        }
    }

    fun clearInbox() {
        while (inbox.tryReceive().isSuccess) { /* drain */ }
    }
}
