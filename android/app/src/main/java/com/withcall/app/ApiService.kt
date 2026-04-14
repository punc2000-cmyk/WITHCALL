package com.withcall.app

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {

    const val SERVER_URL = "https://withcall.pages.dev"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * 연락처 등록여부 업데이트
     * @param deviceId  DB devices.id
     * @param contactIndex  1~5
     * @param status  "변경완료" | "에러"
     */
    /** phase: "119" | "care" */
    fun updateContactStatus(deviceId: Int, contactIndex: Int, status: String, phase: String) {
        val body = JSONObject()
            .put("contactIndex", contactIndex)
            .put("status", status)
            .put("phase", phase)
            .toString()
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url("$SERVER_URL/api/devices/$deviceId/contact-status")
            .patch(body)
            .build()
        client.newCall(request).execute().close()
    }

    fun getDevices(): List<Device> {
        val request = Request.Builder()
            .url("$SERVER_URL/api/devices?status=pending")
            .get()
            .build()

        val body = client.newCall(request).execute().body?.string() ?: "[]"
        val arr = JSONArray(body)

        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Device(
                rowIndex  = o.getInt("id"),
                phone119  = o.optString("phone_119",  ""),
                phoneCare = o.optString("phone_care", ""),
                contact1  = o.optString("contact_1",  ""),
                contact2  = o.optString("contact_2",  ""),
                contact3  = o.optString("contact_3",  ""),
                contact4  = o.optString("contact_4",  ""),
                contact5  = o.optString("contact_5",  "")
            )
        }
    }
}
