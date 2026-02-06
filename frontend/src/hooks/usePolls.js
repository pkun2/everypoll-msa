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
      setPolls(pollsData || [])

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

  useEffect(() => {
    if (pollId) {
      fetchPoll()
    }
  }, [pollId])

  const fetchPoll = async () => {
    try {
      setLoading(true)
      const [pollRes, voteRes] = await Promise.all([
        pollAPI.getById(pollId),
        voteAPI.getMyVote(pollId).catch(() => null)
      ])
      setPoll(pollRes.data)
      setMyVote(voteRes?.data || null)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const vote = async (optionId) => {
    const response = await voteAPI.vote(pollId, optionId)
    await fetchPoll()
    return response.data
  }

  const cancelVote = async () => {
    await voteAPI.cancelVote(pollId)
    setMyVote(null)
    await fetchPoll()
  }

  const deletePoll = async () => {
    await pollAPI.delete(pollId)
  }

  return { poll, myVote, loading, error, vote, cancelVote, deletePoll, refetch: fetchPoll }
}