import { useState, useEffect, useCallback } from 'react'
import { commentAPI } from '../api/client'
import { useAuth } from '../context/AuthContext'

export function useComments(pollId) {
    const [comments, setComments] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    // Pagination & infinite scroll states
    const [page, setPage] = useState(0)
    const [hasNext, setHasNext] = useState(false)
    const [isFetchingNextPage, setIsFetchingNextPage] = useState(false)

    const { user } = useAuth()

    // Function to load comments
    const fetchComments = useCallback(async (pageToLoad = 0, isLoadMore = false) => {
        try {
            if (!isLoadMore) {
                setLoading(true)
            } else {
                setIsFetchingNextPage(true)
            }

            const response = await commentAPI.getComments(pollId, pageToLoad)
            const data = response.data

            // Assuming response is a Spring Data Slice
            const newComments = data.content || []

            if (isLoadMore) {
                setComments(prev => [...prev, ...newComments])
            } else {
                setComments(newComments)
            }

            setHasNext(data.hasNext || !data.last)
            setPage(pageToLoad)

        } catch (err) {
            setError(err.message || '댓글을 불러오는데 실패했습니다.')
        } finally {
            setLoading(false)
            setIsFetchingNextPage(false)
        }
    }, [pollId])

    // Initial load
    useEffect(() => {
        if (pollId) {
            fetchComments(0)
        }
    }, [pollId, fetchComments])

    const loadMore = () => {
        if (hasNext && !isFetchingNextPage) {
            fetchComments(page + 1, true)
        }
    }

    const addComment = async (content) => {
        if (!user) {
            throw new Error('로그인이 필요합니다.')
        }

        try {
            await commentAPI.createComment(pollId, content)
            // fetch back to page 0 to show the new comment (usually new ones are at the top)
            await fetchComments(0)
        } catch (err) {
            throw new Error(err.response?.data?.message || '댓글 작성에 실패했습니다.')
        }
    }

    const deleteComment = async (commentId) => {
        if (!user) {
            throw new Error('로그인이 필요합니다.')
        }

        try {
            await commentAPI.deleteComment(pollId, commentId)
            // remove from state immediately for better UX
            setComments(prev => prev.filter(c => c.id !== commentId))
        } catch (err) {
            throw new Error(err.response?.data?.message || '댓글 삭제에 실패했습니다.')
        }
    }

    return {
        comments,
        loading,
        error,
        hasNext,
        isFetchingNextPage,
        loadMore,
        addComment,
        deleteComment,
        refetch: () => fetchComments(0)
    }
}
