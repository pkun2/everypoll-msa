import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import apiClient from '../api';

// 1. 타입 정의 (Types)
// ===================================
interface User {
  id: string;
  email: string;
  name: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
}

// 2. 초기 상태 (Initial State)
// ===================================
const initialState: AuthState = {
  user: null,
  token: localStorage.getItem('token'),
  isAuthenticated: !!localStorage.getItem('token'),
  status: 'idle',
  error: null,
};

// 3. 비동기 액션 (Async Thunks / "Operations")
// ===================================
export const loginUser = createAsyncThunk(
  'auth/login', // 액션 타입 문자열
  async (credentials: any, { rejectWithValue }) => {
    try {
      const response = await apiClient.post('/auth/login', credentials);
      localStorage.setItem('token', response.data.token);
      return response.data;
    } catch (err: any) {
      return rejectWithValue(err.response?.data?.message || 'Login failed');
    }
  }
);

// 4. 슬라이스 (Slice: Reducers + Action Creators)
// ===================================
const authSlice = createSlice({
  name: 'auth', // Ducks 패턴에서는 이 name이 액션 타입의 접두사가 됨
  initialState,
  // 동기 액션을 위한 리듀서
  reducers: {
    logout: (state) => {
      state.user = null;
      state.token = null;
      state.isAuthenticated = false;
      localStorage.removeItem('token');
      console.log('User logged out');
    },
  },
  // 비동기 액션을 위한 리듀서
  extraReducers: (builder) => {
    builder
      .addCase(loginUser.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.isAuthenticated = true;
        state.token = action.payload.token;
        state.user = action.payload.user;
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.status = 'failed';
        state.isAuthenticated = false;
        state.error = action.payload as string;
      });
  },
});

// 5. 액션 생성자 내보내기 (Export Action Creators)
// ===================================
export const { logout } = authSlice.actions;

// 6. 리듀서 내보내기 (Export Reducer)
// ===================================
export default authSlice.reducer;

// (선택사항) 셀렉터 내보내기 (Export Selectors)
export const selectIsAuthenticated = (state: { auth: AuthState }) => state.auth.isAuthenticated;
export const selectUser = (state: { auth: AuthState }) => state.auth.user;