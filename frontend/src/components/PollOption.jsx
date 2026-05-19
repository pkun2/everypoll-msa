function PollOption({ option, totalVotes, isSelected, onVote, disabled }) {
  const percentage = totalVotes > 0
    ? Math.round((option.voteCount / totalVotes) * 100)
    : 0

  return (
    <button
      onClick={() => onVote(option.id)}
      disabled={disabled}
      className={`w-full text-left p-4 rounded-lg border-2 transition relative overflow-hidden
        ${isSelected
          ? 'border-blue-500 bg-blue-50'
          : 'border-gray-200 hover:border-gray-300'
        }
        ${disabled ? 'cursor-not-allowed' : 'cursor-pointer'}
      `}
    >
      {/* 투표율 배경 바 */}
      <div
        className={`absolute inset-y-0 left-0 transition-all duration-500
          ${isSelected ? 'bg-blue-100' : 'bg-gray-100'}
        `}
        style={{ width: `${percentage}%` }}
      />

      <div className="relative flex justify-between items-center">
        <span className={`font-medium ${isSelected ? 'text-blue-700' : 'text-gray-900'}`}>
          {isSelected && '✓ '}{option.text}
        </span>
        <span className="text-gray-600">
          {option.voteCount}표 ({percentage}%)
        </span>
      </div>
    </button>
  )
}

export default PollOption