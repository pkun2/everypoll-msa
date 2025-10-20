import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthForm from '../components/AuthForm';
import { useAppSelector } from '../hooks/redux-hooks';
import { selectIsAuthenticated } from '../modules/authSlice';

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  
  // Redux 스토어에서 현재 인증 상태를 가져옵니다.
  const isAuthenticated = useAppSelector(selectIsAuthenticated);

  // useEffect 훅을 사용하여, 이미 로그인된 상태라면 메인 페이지('/')로 리디렉션합니다.
  useEffect(() => {
    if (isAuthenticated) {
      console.log('Already authenticated, redirecting to home page.');
      navigate('/');
    }
  }, [isAuthenticated, navigate]);

  return (
    <div className="login-page-container">
      {/* 
        실제 UI와 로직은 AuthForm 컴포넌트가 모두 처리합니다.
        LoginPage는 이 폼을 페이지에 표시하는 역할만 합니다.
      */}
      <AuthForm />
    </div>
  );
};

export default LoginPage;