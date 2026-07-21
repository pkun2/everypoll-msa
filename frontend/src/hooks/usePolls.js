import { useState, useEffect } from 'react'
import { pollAPI, voteAPI } from '../api/client'

export function usePolls(page = 0) {
  const [polls, setPolls] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    fetchPolls()
  }, [page])

  const fetchPolls = async () => {
    try {
      setLoading(true)
      const response = await pollAPI.getAll(page)
      // Check if response.data is directly the array (based on user provided example)
      // or if it has a content property (Spring Page).
      const pollsData = Array.isArray(response.data) ? response.data : response.data.content
      const visiblePolls = (pollsData || []).filter(poll => !poll.isBlind)
      setPolls(visiblePolls)

      // If pagination info is present, use it. Otherwise default to 1 page.
      setTotalPages(response.data.totalPages || 1)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return { polls, loading, error, totalPages, refetch: fetchPolls }
}

export function usePoll(pollId) {
  const [poll, setPoll] = useState(null)
  const [myVote, setMyVote] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [summaryLoading, setSummaryLoading] = useState(false)

  useEffect(() => {
    if (pollId) {
      fetchPoll()
    }
  }, [pollId])

  const fetchPoll = async (silent = false) => {
    try {
      if (!silent) setLoading(true)
      const [pollRes, voteRes] = await Promise.all([
        pollAPI.getById(pollId),
        voteAPI.getMyVote(pollId).catch(() => null)
      ])
      const pollData = pollRes.data
      if (pollData.isBlind) {
        throw new Error('이 투표는 정책 위반으로 인해 블라인드 처리되었습니다.')
      }

      setPoll(pollData)
      setMyVote(voteRes?.data || null)
      if (!silent) fetchSummary()
    } catch (err) {
      setError(err.message)
    } finally {
      if (!silent) setLoading(false)
    }
  }

  // AI 댓글 요약 응답까지 대기, 따라서 poll 로딩과 분리
  const fetchSummary = async () => {
    setSummaryLoading(true)
    try {
      const res = await pollAPI.getSummary(pollId)
      setPoll((prev) => (prev ? { ...prev, commentSummary: res.data.summary } : prev))
    } catch (err) {
      
    } finally {
      setSummaryLoading(false)
    }
  }

  const vote = async (optionId) => {
    const response = await voteAPI.vote(pollId, optionId)
    // 백엔드 동기화(Kafka)를 위한 짧은 지연 후 데이터 갱신
    setTimeout(() => fetchPoll(true), 300)
    return response.data
  }

  const cancelVote = async () => {
    await voteAPI.cancelVote(pollId)
    setMyVote(null)
    // 백엔드 동기화(Kafka)를 위한 짧은 지연 후 데이터 갱신
    setTimeout(() => fetchPoll(true), 300)
  }

  const deletePoll = async () => {
    await pollAPI.delete(pollId)
  }

  return { poll, myVote, loading, error, summaryLoading, vote, cancelVote, deletePoll, refetch: fetchPoll }
}