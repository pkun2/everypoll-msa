import React, { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useComments } from '../hooks/useComments'

function CommentSection({ pollId }) {
    const { user } = useAuth()
    const {
        comments,
        loading,
        error,
        hasNext,
        isFetchingNextPage,
        loadMore,
        addComment,
        deleteComment
    } = useComments(pollId)

    const [newComment, setNewComment] = useState('')
    const [submitting, setSubmitting] = useState(false)

    const handleSubmit = async (e) => {
        e.preventDefault()
        if (!newComment.trim()) return

        try {
            setSubmitting(true)
            await addComment(newComment)
            setNewComment('')
        } catch (err) {
            alert(err.message)
        } finally {
            setSubmitting(false)
        }
    }

    const handleDelete = async (commentId) => {
        if (window.confirm('댓글을 삭제하시겠습니까?')) {
            try {
                await deleteComment(commentId)
            } catch (err) {
                alert(err.message)
            }
        }
    }

    if (loading && comments.length === 0) {
        return (
            <div className="mt-8 pt-6 border-t border-gray-100 flex justify-center">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
            </div>
        )
    }

    return (
        <div className="mt-8 pt-6 border-t border-gray-100">
            <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                💬 댓글
                {comments.length > 0 && (
                    <span className="text-sm font-normal text-gray-500">
                        조회된 댓글 {comments.length}개
                    </span>
                )}
            </h3>

            {/* 댓글 작성 폼 */}
            <form onSubmit={handleSubmit} className="mb-8">
                <div className="flex gap-3">
                    <input
                        type="text"
                        value={newComment}
                        onChange={(e) => setNewComment(e.target.value)}
                        placeholder={user ? "댓글을 남겨주세요..." : "로그인 후 댓글을 남길 수 있습니다."}
                        disabled={!user || submitting}
                        className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-50 focus:outline-none transition-shadow"
                        required
                    />
                    <button
                        type="submit"
                        disabled={!user || !newComment.trim() || submitting}
                        className="px-6 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        {submitting ? '작성 중...' : '등록'}
                    </button>
                </div>
            </form>

            {/* 에러 메시지 */}
            {error && (
                <div className="text-red-500 mb-4 text-sm bg-red-50 p-3 rounded-lg border border-red-100">
                    오류가 발생했습니다: {error}
                </div>
            )}

            {/* 댓글 목록 */}
            <div className="space-y-4">
                {comments.length === 0 && !error ? (
                    <p className="text-gray-500 text-center py-4 text-sm bg-gray-50 rounded-lg border border-dashed border-gray-200">
                        아직 댓글이 없습니다. 첫 번째 댓글을 남겨보세요!
                    </p>
                ) : (
                    <div className="flex flex-col gap-3">
                        {comments.map((comment) => {
                            const isAuthor = user && comment.userId === user.userId;
                            return (
                                <div
                                    key={comment.id}
                                    className="p-4 bg-gray-50 rounded-lg flex flex-col gap-2 relative group hover:bg-gray-100 transition-colors"
                                >
                                    <p className="text-gray-800 text-sm">{comment.content}</p>
                                    <div className="flex justify-between items-center mt-1">
                                        <div className="flex text-xs text-gray-500 gap-3">
                                            <span className="font-medium text-gray-700">익명 {comment.userId}</span>
                                            <span>{new Date(comment.createdAt).toLocaleString('ko-KR')}</span>
                                        </div>
                                        {isAuthor && (
                                            <button
                                                onClick={() => handleDelete(comment.id)}
                                                className="text-xs text-red-500 opacity-0 group-hover:opacity-100 transition-opacity hover:underline"
                                                title="댓글 삭제"
                                            >
                                                삭제
                                            </button>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            {/* 더보기 버튼 */}
            {hasNext && (
                <div className="mt-6 flex justify-center">
                    <button
                        onClick={loadMore}
                        disabled={isFetchingNextPage}
                        className="px-6 py-2 border border-gray-300 text-gray-600 rounded-full hover:bg-gray-50 hover:text-gray-900 transition-colors disabled:opacity-50 text-sm font-medium flex items-center gap-2"
                    >
                        {isFetchingNextPage ? (
                            <>
                                <div className="w-4 h-4 border-2 border-gray-300 border-t-gray-600 rounded-full animate-spin" />
                                불러오는 중...
                            </>
                        ) : '댓글 더보기 ⬇️'}
                    </button>
                </div>
            )}
        </div>
    )
}

export default CommentSection
