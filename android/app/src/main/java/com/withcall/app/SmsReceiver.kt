package com.withcall.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * SMS 수신 BroadcastReceiver
 * AndroidManifest.xml에 등록되어 있으며, SMS_RECEIVED 인텐트를 처리함
 * 수신된 메시지를 SmsProcessor.instance로 전달
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in messages) {
            val sender = msg.originatingAddress ?: continue
            val body = msg.messageBody ?: continue
            SmsProcessor.instance?.onSmsReceived(sender, body)
        }
    }
}
