import { configureStore } from '@reduxjs/toolkit';

import authReducer from '../modules/authSlice';
import pollsReducer from '../modules/pollsSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    polls: pollsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;