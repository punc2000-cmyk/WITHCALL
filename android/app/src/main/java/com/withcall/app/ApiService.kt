package com.withcall.app

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * 서버 API 통신 (OkHttp 사용)
 * - getDevices: 최신 배치의 pending 단말기 목록 조회
 * - markComplete: 단말기 업무완료 처리
 */
object ApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * pending 상태의 단말기 목록을 서버에서 조회
     */
    fun getDevices(serverUrl: String): List<Device> {
        val url = "${serverUrl.trimEnd('/')}/api/devices?status=pending"
        val request = Request.Builder().url(url).get().build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: "[]"

        val arr = JSONArray(body)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            Device(
                id          = obj.getInt("id"),
                phoneNumber = obj.getString("phone_number"),
                msgB        = obj.optString("msg_b", ""),
                msgC        = obj.optString("msg_c", ""),
                msgD        = obj.optString("msg_d", ""),
                msgE        = obj.optString("msg_e", ""),
                msgF        = obj.optString("msg_f", ""),
                status      = obj.optString("status", "pending")
            )
        }
    }

    /**
     * 단말기 업무완료 처리 (status = completed)
     */
    fun markComplete(serverUrl: String, deviceId: Int) {
        val url = "${serverUrl.trimEnd('/')}/api/devices/$deviceId/complete"
        val body = "{}".toRequestBody(JSON_TYPE)
        val request = Request.Builder().url(url).patch(body).build()
        client.newCall(request).execute().close()
    }
}
