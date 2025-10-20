import React from 'react';

interface Poll {
  id: string;
  title: string;
  author: string;
  createdAt: string;
}

const PollItem: React.FC<{ poll: Poll }> = ({ poll }) => {
  return (
    <div className="poll-card">
      <h3>{poll.title}</h3>
      <p>by {poll.author}</p>
      <span>Created on: {new Date(poll.createdAt).toLocaleDateString()}</span>
      <button className="btn btn-secondary">Vote Now</button>
    </div>
  );
};

export default PollItem;