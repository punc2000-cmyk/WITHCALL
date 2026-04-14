package com.withcall.app

/** 개별 연락처 등록 상태 */
class ContactState(val number: String) {
    var shown: Boolean = false   // 화면에 표시됨
    var done: Boolean  = false   // "변경완료" 녹색 버튼 표시
}

/** RecyclerView 항목 — 단말기 1행의 전체 UI 상태 */
class DeviceItem(val device: Device) {

    // ── 119비상벨 (D열) ───────────────────────────────────────────
    var smsSent119:       Boolean = false   // TEXT1 전송 완료
    var replyReceived119: Boolean = false   // "현 상태 그룹1 기능 모드입니다." 수신

    val contacts119: List<ContactState> = device.contacts().map { ContactState(it) }

    // ── 돌봄비상벨 (F열) ──────────────────────────────────────────
    var smsSentCare:       Boolean = false
    var replyReceivedCare: Boolean = false

    val contactsCare: List<ContactState> = device.contacts().map { ContactState(it) }
}
