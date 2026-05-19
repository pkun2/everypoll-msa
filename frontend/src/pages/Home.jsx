import { useState } from 'react'
import { usePolls } from '../hooks/usePolls'
import PollCard from '../components/PollCard'

function Home() {
  const [page, setPage] = useState(0)
  const { polls, loading, error, totalPages } = usePolls(page)

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-500">오류가 발생했습니다: {error}</p>
      </div>
    )
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">🗳️ 진행 중인 투표</h1>

      {polls.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-xl">
          <p className="text-gray-500">아직 투표가 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {polls.map((poll) => (
            <PollCard key={poll.id} poll={poll} />
          ))}
        </div>
      )}

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-8">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-4 py-2 rounded-lg border disabled:opacity-50"
          >
            이전
          </button>
          <span className="px-4 py-2">
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 rounded-lg border disabled:opacity-50"
          >
            다음
          </button>
        </div>
      )}
    </div>
  )
}

export default Home