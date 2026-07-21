import axios from 'axios'

const API_URL = import.meta.env.VITE_API_URL || ''

const client = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

// 응답 인터셉터 - 401/403 에러 처리
client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // 401 Unauthorized: 토큰 만료 상황
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            return client(originalRequest)
          })
          .catch((err) => {
            return Promise.reject(err)
          })
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        isRefreshing = false
        localStorage.removeItem('accessToken')
        localStorage.removeItem('user')
        window.location.href = '/login'
        return Promise.reject(error)
      }

      try {
        const response = await axios.post(`${API_URL}/api/auth/refresh`, {
          refreshToken
        })
        const { accessToken, refreshToken: newRefreshToken } = response.data

        localStorage.setItem('accessToken', accessToken)
        localStorage.setItem('refreshToken', newRefreshToken)

        client.defaults.headers.common.Authorization = `Bearer ${accessToken}`
        originalRequest.headers.Authorization = `Bearer ${accessToken}`

        processQueue(null, accessToken)
        return client(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 403 Forbidden: 권한 부족 상황
    if (error.response?.status === 403) {
      alert('해당 요청에 대한 권한이 없습니다.')
    }

    return Promise.reject(error)
  }
)

// Auth API
export const authAPI = {
  register: (data) => client.post('/api/auth/signup', data),
  login: (data) => client.post('/api/auth/login', data),
  logout: () => client.post('/api/auth/logout'),
  refresh: (refreshToken) => client.post('/api/auth/refresh', { refreshToken }),
  me: () => client.get('/api/users/me')
}

// Poll API
export const pollAPI = {
  getAll: (page = 0, size = 10) =>
    client.get(`/api/polls?page=${page}&size=${size}`),
  getById: (id) => client.get(`/api/polls/${id}`),
  getSummary: (id) => client.get(`/api/polls/${id}/summary`),
  create: (data) => client.post('/api/polls', data),
  update: (id, data) => client.put(`/api/polls/${id}`, data),
  delete: (id) => client.delete(`/api/polls/${id}`)
}

// Vote API
export const voteAPI = {
  vote: (pollId, optionId) =>
    client.post('/api/votes', { pollId, optionId }),
  cancelVote: (pollId) =>
    client.delete(`/api/votes/polls/${pollId}`),
  getMyVote: (pollId) =>
    client.get(`/api/votes/polls/${pollId}/me`)
}

// Comment API
export const commentAPI = {
  getComments: (pollId, page = 0, size = 10) =>
    client.get(`/api/polls/${pollId}/comments?page=${page}&size=${size}`),
  createComment: (pollId, content) =>
    client.post(`/api/polls/${pollId}/comments`, { content }),
  deleteComment: (pollId, commentId) =>
    client.delete(`/api/polls/${pollId}/comments/${commentId}`)
}

// RAG API
export const ragAPI = {
  chat: (data) => client.post('/api/v1/chat', data),
  addDocuments: (data) => client.post('/api/v1/documents', data),
  search: (query, k) => client.get(`/api/v1/search?query=${query}&k=${k || 5}`),
  getDocuments: () => client.get('/api/v1/documents')
}

export default client