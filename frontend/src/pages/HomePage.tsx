import React from 'react';
import Header from '../components/Header';
import PollList from '../components/PollList';

const HomePage: React.FC = () => {
  return (
    <div>
      <Header />
      <main className="main-container">
        <PollList />
      </main>
    </div>
  );
};

export default HomePage;