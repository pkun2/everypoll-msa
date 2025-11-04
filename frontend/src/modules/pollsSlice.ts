import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import apiClient from '../api';

// 타입
interface Poll {
  id: string;
  title: string;
  author: string;
  createdAt: string;
}

interface PollsState {
  polls: Poll[];
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
}

// 초기
const initialState: PollsState = {
  polls: [],
  status: 'idle',
  error: null,
};

// 액
export const fetchPolls = createAsyncThunk(
  'polls/fetchAll', // 액션 타입 문자열
  async (_, { rejectWithValue }) => {
    try {
      const response = await apiClient.get('/polls');
      return response.data;
    } catch (err: any) {
      return rejectWithValue(err.response?.data?.message || 'Failed to fetch polls');
    }
  }
);

// 슬라이스
const pollsSlice = createSlice({
  name: 'polls',
  initialState,
  reducers: {
    // 만약 투표 추가, 삭제 등 동기 액션이스필요하다면 여기에 추가
    // 예: pollAdded(state, action: PayloadAction<Poll>) { ... }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchPolls.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchPolls.fulfilled, (state, action: PayloadAction<Poll[]>) => {
        state.status = 'succeeded';
        state.polls = action.payload;
      })
      .addCase(fetchPolls.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload as string;
      });
  },
});

// 5. 액션 생성자 내보내기 (Export Action Creators)
// ===================================
// export const { pollAdded } = pollsSlice.actions;

export default pollsSlice.reducer;

export const selectAllPolls = (state: { polls: PollsState }) => state.polls.polls;
export const selectPollsStatus = (state: { polls: PollsState }) => state.polls.status;