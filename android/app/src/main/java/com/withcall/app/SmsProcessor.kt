package com.withcall.app

import android.content.Context
import android.telephony.SmsManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

/**
 * SMS 송수신 처리 클래스
 * - companion object의 instance를 통해 SmsReceiver가 수신 메시지를 전달
 * - waitForResponse: 특정 번호에서 10자 미만 문자 수신을 60초 대기
 */
class SmsProcessor {

    // 수신된 SMS를 담는 채널 (BUFFERED: 여러 메시지 누적 가능)
    private val inbox = Channel<Pair<String, String>>(Channel.BUFFERED)

    companion object {
        /** SmsReceiver가 수신 메시지를 전달할 싱글톤 인스턴스 */
        var instance: SmsProcessor? = null

        /**
         * 전화번호 정규화: 공백, 하이픈 제거, +82를 0으로 변환
         */
        fun normalizeNumber(number: String): String {
            var n = number.trim().replace(" ", "").replace("-", "")
            if (n.startsWith("+82")) n = "0" + n.substring(3)
            return n
        }

        /**
         * SMS 전송
         */
        fun sendSms(context: Context, phoneNumber: String, message: String) {
            val smsManager = context.getSystemService(SmsManager::class.java)
            // 메시지가 길면 자동 분할 전송
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
        }
    }

    /**
     * SmsReceiver에서 호출 - 수신 메시지를 채널에 넣음
     */
    fun onSmsReceived(sender: String, message: String) {
        inbox.trySend(Pair(sender, message))
    }

    /**
     * fromNumber에서 10자 미만 메시지를 최대 timeoutMs 밀리초 동안 대기
     * @return 수신된 메시지 문자열, 타임아웃 시 null
     */
    suspend fun waitForResponse(fromNumber: String, timeoutMs: Long): String? {
        val target = normalizeNumber(fromNumber)
        return withTimeoutOrNull(timeoutMs) {
            var result: String? = null
            while (result == null) {
                val (sender, message) = inbox.receive()
                if (normalizeNumber(sender) == target && message.trim().length < 10) {
                    result = message.trim()
                }
            }
            result
        }
    }

    /**
     * 채널 초기화 (새 업무처리 시작 전 잔여 메시지 제거)
     */
    fun clearInbox() {
        while (inbox.tryReceive().isSuccess) { /* drain */ }
    }
}
