import React from 'react';
import { useAppDispatch, useAppSelector } from '../hooks/redux-hooks';
import { logout } from '../modules/authSlice';

const Header: React.FC = () => {
const dispatch = useAppDispatch();
const { isAuthenticated, user } = useAppSelector((state) => state.auth);

return (
    <header className="header">
        <div className="header-content">
            <span className="logo">EveryPoll</span>
            <nav>
            {isAuthenticated ? (
                <>
                <span>안녕하세요, {user?.name}님.</span>
                <button onClick={() => dispatch(logout())} className="btn btn-tertiary">
                    Logout
                </button>
                </>
            ) : (
                <span>로그인</span>
            )}
            </nav>
        </div>
    </header>
);
};

export default Header;