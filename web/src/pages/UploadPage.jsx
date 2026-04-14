import { useState } from 'react'
import * as XLSX from 'xlsx'

// "해당없음" / "해당 없음" / "-" → 빈 문자열로 정규화
const NONE_PATTERNS = ['해당없음', '해당 없음', '-']
const clean = (v) => {
  const s = String(v ?? '').trim()
  return NONE_PATTERNS.includes(s) ? '' : s
}

const HEADERS = [
  { label: 'A  지역',          key: 'region' },
  { label: 'B  학교명',        key: 'schoolName' },
  { label: 'C  주소',          key: 'address' },
  { label: 'D  119비상벨',     key: 'phone119' },
  { label: 'E  등록여부',      key: 'registered119' },
  { label: 'F  돌봄비상벨',    key: 'phoneCare' },
  { label: 'G  등록여부',      key: 'registeredCare' },
  { label: 'H  비상연락처1',   key: 'contact1' },
  { label: 'I  비상연락처2',   key: 'contact2' },
  { label: 'J  비상연락처3',   key: 'contact3' },
  { label: 'K  비상연락처4',   key: 'contact4' },
  { label: 'L  비상연락처5',   key: 'contact5' },
]

function rowToDevice(row) {
  return {
    region:         clean(row[0]),
    schoolName:     clean(row[1]),
    address:        clean(row[2]),
    phone119:       clean(row[3]),
    registered119:  clean(row[4]),
    phoneCare:      clean(row[5]),
    registeredCare: clean(row[6]),
    contact1:       clean(row[7]),
    contact2:       clean(row[8]),
    contact3:       clean(row[9]),
    contact4:       clean(row[10]),
    contact5:       clean(row[11]),
  }
}

export default function UploadPage() {
  const [file, setFile] = useState(null)
  const [preview, setPreview] = useState([])
  const [totalRows, setTotalRows] = useState(0)
  const [status, setStatus] = useState(null)
  const [serverUrl, setServerUrl] = useState(localStorage.getItem('wc_server_url') || '')
  const [loading, setLoading] = useState(false)

  const parseRows = (binaryStr) => {
    const wb = XLSX.read(binaryStr, { type: 'binary' })
    const ws = wb.Sheets[wb.SheetNames[0]]
    // 전체 행 (header: 1 = 배열 형태)
    const all = XLSX.utils.sheet_to_json(ws, { header: 1 })

    // 2행(index 1)이 실제 컬럼 헤더 ("지역", "학교명", ...)
    // 데이터는 3행(index 2)부터
    const dataRows = all.slice(2).filter(r => r.some(c => c !== null && c !== undefined && c !== ''))
    return dataRows
  }

  const handleFile = (e) => {
    const f = e.target.files[0]
    if (!f) return
    setFile(f)
    setStatus(null)

    const reader = new FileReader()
    reader.onload = (evt) => {
      const rows = parseRows(evt.target.result)
      setTotalRows(rows.length)
      setPreview(rows.slice(0, 5).map(rowToDevice))
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
        const rows = parseRows(evt.target.result)
        const devices = rows.map(rowToDevice).filter(d => d.phone119 !== '' || d.phoneCare !== '')

        const res = await fetch(`${serverUrl.trim()}/api/upload`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ devices, batchName: file.name })
        })
        const result = await res.json()

        if (result.success) {
          setStatus({ type: 'success', msg: `✅ 업로드 완료: ${result.count}개 등록 (배치 ID: ${result.batchId})` })
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
        style={{ marginBottom: '12px' }}
      />

      <label>엑셀 파일 선택 (.xlsx / .xls)</label>
      <input type="file" accept=".xlsx,.xls" onChange={handleFile} />

      {preview.length > 0 && (
        <div style={{ marginBottom: '16px' }}>
          <h3>미리보기 (최대 5행 / 전체 {totalRows}행)</h3>
          <p style={{ fontSize: '12px', color: '#888', marginBottom: '8px' }}>
            ※ "해당없음" 값은 빈 셀로 처리됩니다
          </p>
          <div style={{ overflowX: 'auto' }}>
            <table>
              <thead>
                <tr>
                  {HEADERS.map(h => <th key={h.key} style={{ whiteSpace: 'nowrap' }}>{h.label}</th>)}
                </tr>
              </thead>
              <tbody>
                {preview.map((d, i) => (
                  <tr key={i}>
                    <td>{d.region}</td>
                    <td>{d.schoolName}</td>
                    <td style={{ maxWidth: '160px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{d.address}</td>
                    <td style={{ color: d.phone119 ? '#1A73E8' : '#ccc' }}>{d.phone119 || '—'}</td>
                    <td>{d.registered119}</td>
                    <td style={{ color: d.phoneCare ? '#1A73E8' : '#ccc' }}>{d.phoneCare || '—'}</td>
                    <td>{d.registeredCare}</td>
                    <td>{d.contact1}</td>
                    <td>{d.contact2}</td>
                    <td>{d.contact3}</td>
                    <td>{d.contact4}</td>
                    <td>{d.contact5}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
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
