CREATE DATABASE IF NOT EXISTS vote;

CREATE USER 'everyvote_springboot'@'%' IDENTIFIED BY 'VoteSpring2014';

GRANT ALL PRIVILEGES ON vote.* TO 'everyvote_springboot'@'%';

FLUSH PRIVILEGES;
