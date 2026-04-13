package com.withcall.app

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.withcall.app.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val smsProcessor = SmsProcessor()

    private var devices: List<Device> = emptyList()
    private var processingJob: Job? = null
    private var isProcessing = false

    companion object {
        private const val PREFS = "withcall_prefs"
        private const val KEY_TEXT1 = "text1"
        private const val KEY_URL = "server_url"
        private const val PERM_CODE = 100
        private const val TIMEOUT_MS = 60_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SmsProcessor.instance = smsProcessor

        loadPrefs()
        checkPermissions()

        binding.btnLoad.setOnClickListener { loadDevices() }
        binding.btnProcess.setOnClickListener {
            if (isProcessing) stopProcessing() else startProcessing()
        }
    }

    // ── 설정 저장/불러오기 ─────────────────────────────────────────────────

    private fun loadPrefs() {
        val p = getSharedPreferences(PREFS, MODE_PRIVATE)
        binding.etServerUrl.setText(p.getString(KEY_URL, ""))
        binding.etText1.setText(p.getString(KEY_TEXT1, ""))
    }

    private fun savePrefs() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().apply {
            putString(KEY_URL, binding.etServerUrl.text.toString().trim())
            putString(KEY_TEXT1, binding.etText1.text.toString().trim())
            apply()
        }
    }

    // ── 권한 요청 ──────────────────────────────────────────────────────────

    private fun checkPermissions() {
        val perms = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERM_CODE)
        }
    }

    // ── 단말기 목록 불러오기 ───────────────────────────────────────────────

    private fun loadDevices() {
        val url = binding.etServerUrl.text.toString().trim()
        if (url.isEmpty()) { toast("서버 URL을 입력해주세요"); return }
        savePrefs()
        log("서버에서 단말기 목록 불러오는 중...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val list = ApiService.getDevices(url)
                withContext(Dispatchers.Main) {
                    devices = list
                    binding.tvCount.text = "로드된 단말기: ${list.size}대"
                    binding.btnProcess.isEnabled = list.isNotEmpty()
                    log("${list.size}개 단말기 로드 완료")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    log("오류: ${e.message}")
                    toast("불러오기 실패: ${e.message}")
                }
            }
        }
    }

    // ── 업무처리 시작/중지 ─────────────────────────────────────────────────

    private fun startProcessing() {
        val text1 = binding.etText1.text.toString().trim()
        val url   = binding.etServerUrl.text.toString().trim()
        if (text1.isEmpty()) { toast("전송 TEXT 1을 입력해주세요"); return }
        if (url.isEmpty())   { toast("서버 URL을 입력해주세요"); return }

        savePrefs()
        smsProcessor.clearInbox()
        isProcessing = true
        binding.btnProcess.text = "중지"
        binding.btnLoad.isEnabled = false
        log("\n===== 업무처리 시작 =====")

        processingJob = lifecycleScope.launch {
            var successCount = 0
            for ((index, device) in devices.withIndex()) {
                if (!isProcessing) break
                updateStatus("처리 중 ${index + 1}/${devices.size}  |  ${device.phoneNumber}")
                log("\n[${index + 1}번] ${device.phoneNumber}")

                val ok = processDevice(device, text1, url)
                if (ok) successCount++
                if (!isProcessing) break
            }
            withContext(Dispatchers.Main) {
                isProcessing = false
                binding.btnProcess.text = "업무처리 시작"
                binding.btnLoad.isEnabled = true
                updateStatus("완료 — 성공: $successCount / ${devices.size}")
                log("\n===== 업무처리 종료 =====")
            }
        }
    }

    private fun stopProcessing() {
        isProcessing = false
        processingJob?.cancel()
        binding.btnProcess.text = "업무처리 시작"
        binding.btnLoad.isEnabled = true
        updateStatus("중지됨")
        log("처리 중지됨")
    }

    // ── 단말기 1대 처리 (6단계 SMS 플로우) ────────────────────────────────

    /**
     * @return true = 정상완료 또는 다음 번호 진행 선택, false = 대기 선택(전체 중단)
     */
    private suspend fun processDevice(device: Device, text1: String, serverUrl: String): Boolean {
        // 단계: (단계명, 전송할 메시지)
        val steps = listOf(
            "1단계(TEXT1)" to text1,
            "2단계(B)"    to device.msgB,
            "3단계(C)"    to device.msgC,
            "4단계(D)"    to device.msgD,
            "5단계(E)"    to device.msgE,
            "6단계(F)"    to device.msgF,
        )

        for ((stepIdx, step) in steps.withIndex()) {
            if (!isProcessing) return false
            val (stepName, message) = step
            if (message.isEmpty()) { log("  [$stepName] 메시지 없음 - 건너뜀"); continue }

            log("  [$stepName] 전송: \"$message\"")
            withContext(Dispatchers.IO) {
                SmsProcessor.sendSms(applicationContext, device.phoneNumber, message)
            }

            val response = smsProcessor.waitForResponse(device.phoneNumber, TIMEOUT_MS)

            if (response == null) {
                // 60초 타임아웃
                log("  [$stepName] 60초 응답 없음")
                val continueNext = showTimeoutDialog(device.phoneNumber, stepName)
                return if (continueNext) {
                    log("  → 다음 번호로 진행")
                    true
                } else {
                    log("  → 대기 선택 - 처리 중단")
                    false
                }
            }

            log("  [$stepName] 응답 수신: \"$response\"")

            // 마지막 단계(F) 응답 수신 → 업무완료 처리
            val isLastStep = stepIdx == steps.size - 1
            if (isLastStep) {
                try {
                    withContext(Dispatchers.IO) { ApiService.markComplete(serverUrl, device.id) }
                    log("  → 업무완료 처리됨")
                } catch (e: Exception) {
                    log("  → 완료 처리 오류: ${e.message}")
                }
            }
        }
        return true
    }

    // ── 타임아웃 다이얼로그 ────────────────────────────────────────────────

    /**
     * @return true = "진행" 선택(다음 번호로), false = "대기" 선택(전체 중단)
     */
    private suspend fun showTimeoutDialog(phone: String, step: String): Boolean =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("응답이 없습니다")
                    .setMessage("[$step] $phone\n\n대기할까요? 다음 번호 진행할까요?")
                    .setPositiveButton("대기") { _, _ -> cont.resume(false) }
                    .setNegativeButton("진행") { _, _ -> cont.resume(true) }
                    .setCancelable(false)
                    .show()
            }
        }

    // ── UI 헬퍼 ───────────────────────────────────────────────────────────

    private fun updateStatus(msg: String) {
        binding.tvStatus.text = msg
    }

    private fun log(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        runOnUiThread {
            binding.tvLog.append("[$time] $msg\n")
            // 스크롤 맨 아래로
            val scroll = binding.scrollLog
            scroll.post { scroll.fullScroll(android.view.View.FOCUS_DOWN) }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        SmsProcessor.instance = null
    }
}
