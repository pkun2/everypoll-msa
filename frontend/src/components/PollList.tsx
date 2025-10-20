import React, { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../hooks/redux-hooks';
import { fetchPolls } from '../modules/pollsSlice';
import PollItem from './PollItem';

const PollList: React.FC = () => {
  const dispatch = useAppDispatch();
  const { polls, status, error } = useAppSelector((state) => state.polls);

  useEffect(() => {
    if (status === 'idle') {
      dispatch(fetchPolls());
    }
  }, [status, dispatch]);

  if (status === 'loading') return <div className="loader">Loading polls...</div>;
  if (status === 'failed') return <p className="error-message">Error: {error}</p>;

  return (
    <div className="poll-list-container">
      <h2>Active Polls</h2>
      <div className="poll-grid">
        {polls.map((poll) => (
          <PollItem key={poll.id} poll={poll} />
        ))}
      </div>
    </div>
  );
};

export default PollList;