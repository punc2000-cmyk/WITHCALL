import { useState } from 'react'
import * as XLSX from 'xlsx'

export default function UploadPage() {
  const [file, setFile] = useState(null)
  const [preview, setPreview] = useState([])
  const [status, setStatus] = useState(null)
  const [serverUrl, setServerUrl] = useState(localStorage.getItem('wc_server_url') || '')
  const [loading, setLoading] = useState(false)

  const handleFile = (e) => {
    const f = e.target.files[0]
    if (!f) return
    setFile(f)
    setStatus(null)

    const reader = new FileReader()
    reader.onload = (evt) => {
      const wb = XLSX.read(evt.target.result, { type: 'binary' })
      const ws = wb.Sheets[wb.SheetNames[0]]
      const rows = XLSX.utils.sheet_to_json(ws, { header: 1 }).filter(r => r.length > 0)
      // 첫 행이 헤더인지 확인 (숫자가 아닌 텍스트면 헤더로 간주)
      const start = isNaN(String(rows[0]?.[0])) ? 1 : 0
      setPreview(rows.slice(start, start + 5))
    }
    reader.readAsBinaryString(f)
  }

  const handleUpload = async () => {
    if (!file) { alert('파일을 선택해주세요'); return }
    if (!serverUrl.trim()) { alert('서버 URL을 입력해주세요'); return }

    localStorage.setItem('wc_server_url', serverUrl.trim())
    setLoading(true)
    setStatus({ type: 'info', msg: '업로드 중...' })

    const reader = new FileReader()
    reader.onload = async (evt) => {
      try {
        const wb = XLSX.read(evt.target.result, { type: 'binary' })
        const ws = wb.Sheets[wb.SheetNames[0]]
        const rows = XLSX.utils.sheet_to_json(ws, { header: 1 }).filter(r => r.length > 0)
        const start = isNaN(String(rows[0]?.[0])) ? 1 : 0
        const dataRows = rows.slice(start)

        const devices = dataRows
          .map(row => ({
            phoneNumber: String(row[0] || '').trim(),
            msgB: String(row[1] || '').trim(),
            msgC: String(row[2] || '').trim(),
            msgD: String(row[3] || '').trim(),
            msgE: String(row[4] || '').trim(),
            msgF: String(row[5] || '').trim(),
          }))
          .filter(d => d.phoneNumber !== '')

        const res = await fetch(`${serverUrl.trim()}/api/upload`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ devices, batchName: file.name })
        })
        const result = await res.json()

        if (result.success) {
          setStatus({ type: 'success', msg: `✅ 업로드 완료: ${result.count}개 단말기 등록 (배치 ID: ${result.batchId})` })
        } else {
          setStatus({ type: 'error', msg: `❌ 오류: ${result.error}` })
        }
      } catch (e) {
        setStatus({ type: 'error', msg: `❌ 연결 오류: ${e.message}` })
      } finally {
        setLoading(false)
      }
    }
    reader.readAsBinaryString(file)
  }

  return (
    <div>
      <h2>엑셀 업로드</h2>

      <label>서버 URL</label>
      <input
        type="text"
        value={serverUrl}
        onChange={e => setServerUrl(e.target.value)}
        placeholder="https://withcall.pages.dev"
      />

      <label>엑셀 파일 선택 (.xlsx / .xls)</label>
      <input type="file" accept=".xlsx,.xls" onChange={handleFile} />

      {preview.length > 0 && (
        <div style={{ marginBottom: '16px' }}>
          <h3>미리보기 (최대 5행)</h3>
          <p style={{ fontSize: '12px', color: '#888', marginBottom: '8px' }}>
            A열: M2M 전화번호 | B~F열: 순차 전송 문구
          </p>
          <table>
            <thead>
              <tr>
                {['A (전화번호)', 'B', 'C', 'D', 'E', 'F'].map(h => <th key={h}>{h}</th>)}
              </tr>
            </thead>
            <tbody>
              {preview.map((row, i) => (
                <tr key={i}>
                  {[0,1,2,3,4,5].map(j => <td key={j}>{row[j] || ''}</td>)}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <button onClick={handleUpload} disabled={!file || loading}>
        {loading ? '업로드 중...' : '업로드'}
      </button>

      {status && (
        <div className={`status-box ${status.type}`} style={{ marginTop: '16px' }}>
          {status.msg}
        </div>
      )}
    </div>
  )
}
