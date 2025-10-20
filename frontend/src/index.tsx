import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { Provider } from 'react-redux';
import { store } from './app/store'; // 1. 스토어를 가져옵니다.
import { setupInterceptors } from './api'; // 2. 인터셉터 설정 함수를 가져옵니다.

// 3. 앱이 렌더링되기 전에 인터셉터를 설정합니다.
// store를 apiClient에게 "주입"해줍니다.
setupInterceptors(store);

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);

root.render(
  <React.StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </React.StrictMode>
);