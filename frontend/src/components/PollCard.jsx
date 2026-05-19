import { Link } from 'react-router-dom'

function PollCard({ poll }) {
  const totalVotes = poll.options?.reduce((sum, opt) => sum + opt.voteCount, 0) || 0
  const isEnded = new Date(poll.endAt) < new Date()

  return (
    <Link
      to={`/polls/${poll.id}`}
      className="block bg-white rounded-xl shadow-sm border hover:shadow-md transition p-6"
    >
      <div className="flex justify-between items-start mb-3">
        <h3 className="text-lg font-semibold text-gray-900">{poll.title}</h3>
        {isEnded && (
          <span className="bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded">
            종료됨
          </span>
        )}
      </div>
      
      {poll.description && (
        <p className="text-gray-600 text-sm mb-4 line-clamp-2">
          {poll.description}
        </p>
      )}

      <div className="flex items-center justify-between text-sm text-gray-500">
        <span>🗳️ {totalVotes}명 참여</span>
        <span>📅 {new Date(poll.endAt).toLocaleDateString('ko-KR')}</span>
      </div>
    </Link>
  )
}

export default PollCard