import { useState } from 'react'
import UploadPage from './pages/UploadPage'
import DashboardPage from './pages/DashboardPage'

export default function App() {
  const [page, setPage] = useState('upload')

  return (
    <div className="container">
      <header>
        <h1>WITHCALL SMS 자동화 관리</h1>
        <nav>
          <button
            className={page === 'upload' ? 'active' : ''}
            onClick={() => setPage('upload')}
          >
            엑셀 업로드
          </button>
          <button
            className={page === 'dashboard' ? 'active' : ''}
            onClick={() => setPage('dashboard')}
          >
            현황 대시보드
          </button>
        </nav>
      </header>
      <main>
        {page === 'upload' ? <UploadPage /> : <DashboardPage />}
      </main>
    </div>
  )
}
