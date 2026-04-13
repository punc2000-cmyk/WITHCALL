import { useState, useEffect } from 'react'

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

  const pending = devices.filter(d => d.status === 'pending').length
  const completed = devices.filter(d => d.status === 'completed').length

  return (
    <div>
      <h2>현황 대시보드</h2>

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

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>전화번호 (A열)</th>
            <th>B</th>
            <th>C</th>
            <th>D</th>
            <th>E</th>
            <th>F</th>
            <th>상태</th>
            <th>완료시간</th>
          </tr>
        </thead>
        <tbody>
          {devices.map(d => (
            <tr key={d.id}>
              <td>{d.id}</td>
              <td>{d.phone_number}</td>
              <td>{d.msg_b}</td>
              <td>{d.msg_c}</td>
              <td>{d.msg_d}</td>
              <td>{d.msg_e}</td>
              <td>{d.msg_f}</td>
              <td>
                <span className={`badge ${d.status}`}>
                  {d.status === 'completed' ? '완료' : '대기'}
                </span>
              </td>
              <td>{d.completed_at ? new Date(d.completed_at).toLocaleString('ko-KR') : '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {devices.length === 0 && !loading && (
        <p style={{ color: '#888', marginTop: '16px', textAlign: 'center' }}>
          등록된 단말기가 없습니다. 엑셀을 업로드해주세요.
        </p>
      )}
    </div>
  )
}
