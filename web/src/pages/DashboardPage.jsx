import { useState, useEffect } from 'react'
import * as XLSX from 'xlsx'

const Circle = ({ status }) => {
  if (!status) return <span style={{ color: '#ccc' }}>—</span>
  const color = status === '변경완료' ? '#1E7E34' : '#C62828'
  return (
    <span
      title={status}
      style={{
        display: 'inline-block',
        width: 12, height: 12,
        borderRadius: '50%',
        background: color,
        verticalAlign: 'middle'
      }}
    />
  )
}

const CONTACT_LABELS = ['연락처1', '연락처2', '연락처3', '연락처4', '연락처5']

export default function DashboardPage() {
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(false)
  const [serverUrl] = useState(localStorage.getItem('wc_server_url') || '')

  const load = async () => {
    if (!serverUrl) { alert('업로드 페이지에서 서버 URL을 먼저 입력해주세요'); return }
    setLoading(true)
    try {
      const res = await fetch(`${serverUrl}/api/devices`)
      const data = await res.json()
      setDevices(Array.isArray(data) ? data : [])
    } catch (e) {
      alert('불러오기 실패: ' + e.message)
    }
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  const pending   = devices.filter(d => d.status === 'pending').length
  const completed = devices.filter(d => d.status === 'completed').length

  const downloadExcel = () => {
    const rows = []
    devices.forEach(d => {
      const statusLabel = d.status === 'completed' ? '완료' : '대기'
      // 119비상벨 행
      rows.push({
        'ID': d.id,
        '지역': d.region || '',
        '학교명': d.school_name || '',
        '구분': '119비상벨',
        '단말기 번호': d.phone_119 || '',
        '연락처1 번호': d[`contact_1`] || '',
        '연락처1 등록여부': d[`contact_1_status_119`] || '',
        '연락처2 번호': d[`contact_2`] || '',
        '연락처2 등록여부': d[`contact_2_status_119`] || '',
        '연락처3 번호': d[`contact_3`] || '',
        '연락처3 등록여부': d[`contact_3_status_119`] || '',
        '연락처4 번호': d[`contact_4`] || '',
        '연락처4 등록여부': d[`contact_4_status_119`] || '',
        '연락처5 번호': d[`contact_5`] || '',
        '연락처5 등록여부': d[`contact_5_status_119`] || '',
        '상태': statusLabel,
      })
      // 돌봄비상벨 행
      rows.push({
        'ID': '',
        '지역': '',
        '학교명': '',
        '구분': '돌봄비상벨',
        '단말기 번호': d.phone_care || '',
        '연락처1 번호': d[`contact_1`] || '',
        '연락처1 등록여부': d[`contact_1_status_care`] || '',
        '연락처2 번호': d[`contact_2`] || '',
        '연락처2 등록여부': d[`contact_2_status_care`] || '',
        '연락처3 번호': d[`contact_3`] || '',
        '연락처3 등록여부': d[`contact_3_status_care`] || '',
        '연락처4 번호': d[`contact_4`] || '',
        '연락처4 등록여부': d[`contact_4_status_care`] || '',
        '연락처5 번호': d[`contact_5`] || '',
        '연락처5 등록여부': d[`contact_5_status_care`] || '',
        '상태': '',
      })
    })
    const ws = XLSX.utils.json_to_sheet(rows)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '현황')
    XLSX.writeFile(wb, `withcall_현황_${new Date().toISOString().slice(0,10)}.xlsx`)
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
        <h2 style={{ margin: 0 }}>현황 대시보드</h2>
        <button
          onClick={downloadExcel}
          disabled={devices.length === 0}
          style={{ background: '#1E7E34', color: '#fff', border: 'none', borderRadius: '6px', padding: '8px 18px', fontSize: '14px', cursor: 'pointer', fontWeight: 600 }}
        >
          엑셀 다운로드
        </button>
      </div>

      <div className="stats">
        <div className="stat-card total">
          <span className="num">{devices.length}</span>전체
        </div>
        <div className="stat-card pending">
          <span className="num">{pending}</span>대기
        </div>
        <div className="stat-card completed">
          <span className="num">{completed}</span>완료
        </div>
      </div>

      <button onClick={load} disabled={loading} style={{ marginBottom: '16px' }}>
        {loading ? '로딩 중...' : '새로고침'}
      </button>

      <div style={{ overflowX: 'auto' }}>
        <table>
          <thead>
            <tr>
              <th rowSpan={2} style={{ verticalAlign: 'middle' }}>ID</th>
              <th rowSpan={2} style={{ verticalAlign: 'middle' }}>지역</th>
              <th rowSpan={2} style={{ verticalAlign: 'middle' }}>학교명</th>
              <th rowSpan={2} style={{ verticalAlign: 'middle' }}>구분</th>
              <th rowSpan={2} style={{ verticalAlign: 'middle' }}>단말기 번호</th>
              {CONTACT_LABELS.map(l => (
                <th key={l} colSpan={2} style={{ textAlign: 'center' }}>{l}</th>
              ))}
              <th rowSpan={2} style={{ verticalAlign: 'middle' }}>상태</th>
            </tr>
            <tr>
              {CONTACT_LABELS.map(l => (
                <>
                  <th key={`${l}-n`} style={{ fontWeight: 'normal', fontSize: 11 }}>번호</th>
                  <th key={`${l}-s`} style={{ fontWeight: 'normal', fontSize: 11, textAlign: 'center' }}>등록여부</th>
                </>
              ))}
            </tr>
          </thead>
          <tbody>
            {devices.map(d => (
              <>
                {/* ── 119비상벨 행 ── */}
                <tr key={`${d.id}-119`}>
                  <td rowSpan={2} style={{ verticalAlign: 'middle', textAlign: 'center' }}>{d.id}</td>
                  <td rowSpan={2} style={{ verticalAlign: 'middle' }}>{d.region}</td>
                  <td rowSpan={2} style={{ verticalAlign: 'middle', whiteSpace: 'nowrap' }}>{d.school_name}</td>
                  <td style={{ color: '#1A73E8', fontWeight: 600, whiteSpace: 'nowrap' }}>119비상벨</td>
                  <td style={{ color: d.phone_119 ? '#1A73E8' : '#ccc', whiteSpace: 'nowrap', fontFamily: 'monospace' }}>
                    {d.phone_119 || '—'}
                  </td>
                  {[1,2,3,4,5].map(n => (
                    <>
                      <td key={`119-c${n}`} style={{ whiteSpace: 'nowrap', fontFamily: 'monospace', fontSize: 12 }}>
                        {d[`contact_${n}`] || '—'}
                      </td>
                      <td key={`119-s${n}`} style={{ textAlign: 'center' }}>
                        <Circle status={d[`contact_${n}_status_119`]} />
                      </td>
                    </>
                  ))}
                  <td rowSpan={2} style={{ verticalAlign: 'middle', textAlign: 'center' }}>
                    <span className={`badge ${d.status}`}>
                      {d.status === 'completed' ? '완료' : '대기'}
                    </span>
                  </td>
                </tr>

                {/* ── 돌봄비상벨 행 ── */}
                <tr key={`${d.id}-care`} style={{ background: '#fafafa' }}>
                  <td style={{ color: '#5F6368', fontWeight: 600, whiteSpace: 'nowrap' }}>돌봄비상벨</td>
                  <td style={{ color: d.phone_care ? '#555' : '#ccc', whiteSpace: 'nowrap', fontFamily: 'monospace' }}>
                    {d.phone_care || '—'}
                  </td>
                  {[1,2,3,4,5].map(n => (
                    <>
                      <td key={`care-c${n}`} style={{ whiteSpace: 'nowrap', fontFamily: 'monospace', fontSize: 12 }}>
                        {d[`contact_${n}`] || '—'}
                      </td>
                      <td key={`care-s${n}`} style={{ textAlign: 'center' }}>
                        <Circle status={d[`contact_${n}_status_care`]} />
                      </td>
                    </>
                  ))}
                </tr>
              </>
            ))}
          </tbody>
        </table>
      </div>

      {devices.length === 0 && !loading && (
        <p style={{ color: '#888', marginTop: '16px', textAlign: 'center' }}>
          등록된 단말기가 없습니다. 엑셀을 업로드해주세요.
        </p>
      )}
    </div>
  )
}
