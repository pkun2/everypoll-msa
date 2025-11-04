-- 일단 poll 데이터베이스 하나 추가
CREATE DATABASE IF NOT EXISTS poll;

-- springboot 사용자용
CREATE USER 'everypoll_springboot'@'%' IDENTIFIED BY 'Spring2014';
GRANT ALL PRIVILEGES ON poll.* TO 'everypoll_springboot'@'%';
FLUSH PRIVILEGES;
