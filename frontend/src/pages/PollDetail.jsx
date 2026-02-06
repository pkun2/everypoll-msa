import { useParams, useNavigate } from 'react-router-dom'
import { usePoll } from '../hooks/usePolls'
import { useAuth } from '../context/AuthContext'
import PollOption from '../components/PollOption'

function PollDetail() {
  const { pollId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { poll, myVote, loading, error, vote, cancelVote, deletePoll } = usePoll(pollId)

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  if (error || !poll) {
    return (
      <div className="text-center py-12">
        <p className="text-red-500">투표를 찾을 수 없습니다.</p>
        <button
          onClick={() => navigate('/')}
          className="mt-4 text-blue-600 hover:underline"
        >
          홈으로 돌아가기
        </button>
      </div>
    )
  }

  const totalVotes = poll.options?.reduce((sum, opt) => sum + opt.voteCount, 0) || 0
  const isEnded = new Date(poll.endAt) < new Date()
  const hasVoted = !!myVote

  const handleVote = async (optionId) => {
    if (!user) {
      navigate('/login')
      return
    }

    try {
      await vote(optionId)
    } catch (err) {
      alert(err.response?.data?.message || '투표에 실패했습니다.')
    }
  }

  const handleCancelVote = async () => {
    if (window.confirm('투표를 취소하시겠습니까?')) {
      try {
        await cancelVote()
      } catch (err) {
        alert(err.response?.data?.message || '투표 취소에 실패했습니다.')
      }
    }
  }

  const handleDeletePoll = async () => {
    if (window.confirm('정말로 이 투표를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
      try {
        await deletePoll()
        alert('투표가 성공적으로 삭제되었습니다.')
        navigate('/')
      } catch (err) {
        alert(err.response?.data?.message || '투표 삭제에 실패했습니다.')
      }
    }
  }

  const isAuthor = user && poll.createdBy === user.userId

  return (
    <div className="max-w-2xl mx-auto">
      <div className="bg-white rounded-xl shadow-sm border p-8">
        {/* 헤더 */}
        <div className="mb-6">
          <div className="flex items-center gap-2 mb-2">
            {isEnded && (
              <span className="bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded">
                종료됨
              </span>
            )}
            {hasVoted && (
              <span className="bg-blue-100 text-blue-600 text-xs px-2 py-1 rounded">
                투표 완료
              </span>
            )}
          </div>
          <h1 className="text-2xl font-bold text-gray-900">{poll.title}</h1>
          {poll.description && (
            <p className="text-gray-600 mt-2">{poll.description}</p>
          )}
        </div>

        {/* 투표 옵션들 */}
        <div className="space-y-3 mb-6">
          {poll.options?.map((option) => (
            <PollOption
              key={option.id}
              option={option}
              totalVotes={totalVotes}
              isSelected={myVote?.optionId === option.id}
              onVote={handleVote}
              disabled={isEnded || hasVoted}
            />
          ))}
        </div>

        {/* 하단 정보 */}
        <div className="flex items-center justify-between text-sm text-gray-500 pt-4 border-t">
          <span>🗳️ 총 {totalVotes}명 참여</span>
          <span>📅 마감: {new Date(poll.endAt).toLocaleString('ko-KR')}</span>
        </div>

        {/* 투표 취소 버튼 */}
        {hasVoted && !isEnded && (
          <button
            onClick={handleCancelVote}
            className="w-full mt-4 py-2 text-red-600 border border-red-200 rounded-lg hover:bg-red-50 transition"
          >
            투표 취소
          </button>
        )}

        {/* 투표 삭제 버튼 (작성자 전용) */}
        {isAuthor && (
          <button
            onClick={handleDeletePoll}
            className="w-full mt-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition font-medium"
          >
            투표 게시글 삭제
          </button>
        )}
      </div>
    </div>
  )
}

export default PollDetail