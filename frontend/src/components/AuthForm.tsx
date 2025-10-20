import React, { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../hooks/redux-hooks';
import { loginUser } from '../modules/authSlice'; 

const AuthForm: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const dispatch = useAppDispatch();
  const { status, error } = useAppSelector((state) => state.auth);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    dispatch(loginUser({ email, password }));
  };
  
  return (
    <div className="auth-container">
      <div className="auth-form">
        <h2>안녕하세요</h2>
        <p>Enter your credentials to access your account</p>
        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="name@company.com"
              required
            />
          </div>
          <div className="input-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={status === 'loading'}>
            {status === 'loading' ? 'Signing in...' : 'Sign In'}
          </button>
          {error && <p className="error-message">{error}</p>}
        </form>
      </div>
      <div className="auth--info">
        <h1>EveryPoll</h1>
        <p>Your voice, your vote. Create and share polls instantly.</p>
      </div>
    </div>
  );
};

export default AuthForm;