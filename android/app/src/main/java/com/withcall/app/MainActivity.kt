package com.withcall.app

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.withcall.app.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val smsProcessor = SmsProcessor()

    private val deviceItems = mutableListOf<DeviceItem>()
    private lateinit var deviceAdapter: DeviceAdapter

    private var processingJob: Job? = null
    private var isProcessing = false

    companion object {
        private const val PREFS        = "withcall_prefs"
        private const val KEY_TEXT1    = "text1"
        private const val KEY_TEXT2    = "text2"
        private const val PERM_CODE    = 100
        private const val TIMEOUT_MS   = 10_000L   // ① 10초

        private const val REPLY_INIT    = "현 상태 그룹1 기능 모드입니다."
        private const val REPLY_REG     = "입력하신 문자 번호가 등록 되었습니다"
        private const val REPLY_ALREADY = "이미 등록된 전화번호입니다."
        private const val REPLY_EXIT    = "기능 모드를 종료합니다."

        private const val DEFAULT_TEXT1 = "NT+MENU=GN1"
        private const val DEFAULT_TEXT2 = "NT+MENU=OFF"
    }

    // ── 생명주기 ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SmsProcessor.instance = smsProcessor

        setupRecyclerView()
        loadPrefs()
        checkPermissions()

        binding.btnLoad.setOnClickListener { loadDevicesFromServer() }
        binding.btnProcess.setOnClickListener {
            if (isProcessing) stopProcessing() else startProcessing()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SmsProcessor.instance = null
    }

    // ── RecyclerView ──────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(deviceItems)
        binding.rvDevices.layoutManager = LinearLayoutManager(this)
        binding.rvDevices.adapter = deviceAdapter
    }

    // ── 설정 ──────────────────────────────────────────────────────────────

    private fun loadPrefs() {
        val p = getSharedPreferences(PREFS, MODE_PRIVATE)
        binding.etText1.setText(p.getString(KEY_TEXT1, DEFAULT_TEXT1))
        binding.etText2.setText(p.getString(KEY_TEXT2, DEFAULT_TEXT2))
    }

    private fun savePrefs() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().apply {
            putString(KEY_TEXT1, binding.etText1.text.toString().trim())
            putString(KEY_TEXT2, binding.etText2.text.toString().trim())
            apply()
        }
    }

    // ── 권한 ──────────────────────────────────────────────────────────────

    private fun checkPermissions() {
        val needed = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        ).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

        if (needed.isNotEmpty())
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERM_CODE)
    }

    // ── 서버 불러오기 ─────────────────────────────────────────────────────

    private fun loadDevicesFromServer() {
        setLoading(true)
        updateStatus("서버에서 단말기 목록 불러오는 중…")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val devices = ApiService.getDevices()
                withContext(Dispatchers.Main) {
                    deviceItems.clear()
                    deviceItems.addAll(devices.map { DeviceItem(it) })
                    deviceAdapter.notifyDataSetChanged()
                    binding.tvCount.text = "단말기 ${devices.size}대"
                    binding.btnProcess.isEnabled = devices.isNotEmpty()
                    updateStatus("${devices.size}개 단말기 로드 완료")
                    setLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateStatus("불러오기 오류: ${e.message}")
                    toast("서버 연결 실패: ${e.message}")
                    setLoading(false)
                }
            }
        }
    }

    // ── 업무처리 시작 / 중지 ──────────────────────────────────────────────

    private fun startProcessing() {
        val text1 = binding.etText1.text.toString().trim()
        val text2 = binding.etText2.text.toString().trim()
        if (text1.isEmpty()) { toast("전송 TEXT 1을 입력해주세요"); return }

        savePrefs()
        smsProcessor.clearInbox()
        isProcessing = true
        binding.btnProcess.text = "중지"
        binding.btnLoad.isEnabled = false
        setLoading(true)
        updateStatus("업무처리 시작")

        // 상태 초기화
        deviceItems.forEach { item ->
            item.smsSent119 = false; item.replyReceived119 = false
            item.contacts119.forEach { it.shown = false; it.done = false }
            item.smsSentCare = false; item.replyReceivedCare = false
            item.contactsCare.forEach { it.shown = false; it.done = false }
        }
        deviceAdapter.notifyDataSetChanged()

        processingJob = lifecycleScope.launch {
            for ((index, item) in deviceItems.withIndex()) {
                if (!isProcessing) break
                updateStatus("처리 중 ${index + 1}/${deviceItems.size}")

                val ok = processDevice(item, index, text1, text2)
                if (!ok) break
            }
            withContext(Dispatchers.Main) {
                if (isProcessing) {
                    isProcessing = false
                    binding.btnProcess.text = "업무처리 시작"
                    binding.btnLoad.isEnabled = true
                    setLoading(false)
                    updateStatus("완료")
                }
            }
        }
    }

    private fun stopProcessing() {
        isProcessing = false
        processingJob?.cancel()
        binding.btnProcess.text = "업무처리 시작"
        binding.btnLoad.isEnabled = true
        setLoading(false)
        updateStatus("중지됨")
    }

    // ── 단말기 1행 처리 ───────────────────────────────────────────────────

    /**
     * @return true = 다음 행으로 계속, false = 전체 중단
     */
    private suspend fun processDevice(item: DeviceItem, index: Int, text1: String, text2: String): Boolean {
        // ══ Phase 1: 119비상벨 (D열) ═════════════════════════════════
        if (item.device.phone119.isNotEmpty()) {
            val ok = processBell(
                phone       = item.device.phone119,
                label       = "119비상벨",
                phase       = "119",
                text1       = text1,
                text2       = text2,
                index       = index,
                setSent     = { item.smsSent119 = true },
                setReplied  = { item.replyReceived119 = true },
                contacts    = item.contacts119
            )
            if (!ok) return false
        }

        // ══ Phase 2: 돌봄비상벨 (F열) ════════════════════════════════
        if (item.device.phoneCare.isNotEmpty()) {
            val ok = processBell(
                phone       = item.device.phoneCare,
                label       = "돌봄비상벨",
                phase       = "care",
                text1       = text1,
                text2       = text2,
                index       = index,
                setSent     = { item.smsSentCare = true },
                setReplied  = { item.replyReceivedCare = true },
                contacts    = item.contactsCare
            )
            if (!ok) return false
        }

        return true
    }

    /**
     * 단일 비상벨(119 또는 돌봄)에 대해 TEXT1 전송 → 연락처 1~5 등록 프로세스
     * @return true = 성공/다음 진행, false = 전체 중단
     */
    private suspend fun processBell(
        phone: String,
        label: String,
        text1: String,
        text2: String,
        index: Int,
        setSent: () -> Unit,
        setReplied: () -> Unit,
        contacts: List<ContactState>
    ): Boolean {
        // ① TEXT1 전송
        withContext(Dispatchers.IO) { SmsProcessor.sendSms(applicationContext, phone, text1) }
        withContext(Dispatchers.Main) {
            setSent()
            deviceAdapter.notifyItemChanged(index)
            scrollToItem(index)
        }

        // ② "현 상태 그룹1 기능 모드입니다." 대기
        val initReply = smsProcessor.waitForResponse(phone, TIMEOUT_MS, REPLY_INIT)
        if (initReply == null) {
            val cont = showTimeoutDialog(phone, "$label 초기 응답")
            return if (cont) true else { stopProcessing(); false }
        }
        withContext(Dispatchers.Main) {
            setReplied()
            deviceAdapter.notifyItemChanged(index)
        }

        // ③ 연락처 1~5 순서대로 등록
        val labels = listOf("연락처1", "연락처2", "연락처3", "연락처4", "연락처5")
        for (i in 0..4) {
            if (!isProcessing) return false
            val cs = contacts[i]
            if (cs.number.isEmpty()) continue   // empty cell → 다음 연락처

            // 연락처 표시
            withContext(Dispatchers.Main) {
                cs.shown = true
                deviceAdapter.notifyItemChanged(index)
            }

            // NT+PB=<번호> 전송 (하이픈 제거, 숫자만)
            val digits = cs.number.replace("-", "")
            withContext(Dispatchers.IO) {
                SmsProcessor.sendSms(applicationContext, phone, "NT+PB=$digits")
            }

            // "등록 완료" 또는 "이미 등록됨" 수신 → 변경완료 표시
            val regReply = smsProcessor.waitForResponse(
                phone, TIMEOUT_MS, REPLY_REG, REPLY_ALREADY
            )
            if (regReply != null) {
                withContext(Dispatchers.Main) {
                    cs.done = true
                    deviceAdapter.notifyItemChanged(index)
                }
                // 서버 상태 업데이트: 변경완료
                withContext(Dispatchers.IO) {
                    runCatching {
                        ApiService.updateContactStatus(
                            deviceItems[index].device.rowIndex, i + 1, "변경완료"
                        )
                    }
                }
            } else {
                // 타임아웃 → 서버에 에러 기록
                withContext(Dispatchers.IO) {
                    runCatching {
                        ApiService.updateContactStatus(
                            deviceItems[index].device.rowIndex, i + 1, "에러"
                        )
                    }
                }
                val cont = showTimeoutDialog(phone, "$label ${labels[i]} 등록")
                if (!cont) { stopProcessing(); return false }
                // "진행" 선택 → 이 연락처 건너뛰고 다음으로
            }
        }

        // ④ 연락처 전체 완료 → TEXT2(NT+MENU=OFF) 전송
        withContext(Dispatchers.IO) { SmsProcessor.sendSms(applicationContext, phone, text2) }

        // ⑤ "기능 모드를 종료합니다." 확인 후 다음 프로세스
        val exitReply = smsProcessor.waitForResponse(phone, TIMEOUT_MS, REPLY_EXIT)
        if (exitReply == null) {
            val cont = showTimeoutDialog(phone, "$label 종료 확인")
            if (!cont) { stopProcessing(); return false }
        }

        return true
    }

    // ── 타임아웃 다이얼로그 ───────────────────────────────────────────────

    /** @return true = 다음으로 진행, false = 전체 중단 */
    private suspend fun showTimeoutDialog(phone: String, step: String): Boolean =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("응답 없음 (10초 초과)")
                    .setMessage("[$step]\n$phone\n\n다음으로 진행하시겠습니까?")
                    .setPositiveButton("진행") { _, _ -> cont.resume(true) }
                    .setNegativeButton("중단") { _, _ -> cont.resume(false) }
                    .setCancelable(false)
                    .show()
            }
        }

    // ── UI 헬퍼 ───────────────────────────────────────────────────────────

    private fun setLoading(active: Boolean) {
        binding.progressBar.visibility = if (active) View.VISIBLE else View.GONE
    }

    private fun updateStatus(msg: String) {
        binding.tvStatus.text = msg
    }

    private fun scrollToItem(index: Int) {
        binding.rvDevices.smoothScrollToPosition(index)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
