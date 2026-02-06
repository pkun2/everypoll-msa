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

  const user = localStorage.getItem('user')
  if (user) {
    try {
      const userData = JSON.parse(user)
      if (userData.userId) {
        config.headers['X-User-Id'] = userData.userId
      }
    } catch (e) {
      console.error('Failed to parse user data from localStorage', e)
    }
  }

  return config
})

// 응답 인터셉터 - 401 에러 처리
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('user')
      window.location.href = '/login'
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

// RAG API
export const ragAPI = {
  chat: (data) => client.post('/api/v1/chat', data),
  addDocuments: (data) => client.post('/api/v1/documents', data),
  search: (query, k) => client.get(`/api/v1/search?query=${query}&k=${k || 5}`),
  getDocuments: () => client.get('/api/v1/documents')
}

export default client