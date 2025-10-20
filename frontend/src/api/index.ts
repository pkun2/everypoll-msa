import axios from 'axios';
// import { store } from '../app/store'; // 💀 이 줄을 반드시 삭제합니다.

// 1. Axios 인스턴스 생성
const apiClient = axios.create({
  baseURL: process.env.REACT_APP_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 2. 인터셉터를 설정하는 함수를 export 합니다.
// 이 함수는 store를 인자로 받습니다.
export const setupInterceptors = (store: any) => {
  // 요청 인터셉터
  apiClient.interceptors.request.use(
    (config) => {
      // 이제 store는 외부에서 주입받은 것을 사용합니다.
      const token = store.getState().auth.token;
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  // 응답 인터셉터
  apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
      const { status, config: originalRequest } = error.response;

      if (status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;
        console.error("Authentication Error: Logging out.");
        // 외부에서 주입받은 store를 사용하여 dispatch 합니다.
        store.dispatch({ type: 'auth/logout' });
      }
      return Promise.reject(error);
    }
  );
};

export default apiClient;