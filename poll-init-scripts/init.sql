CREATE DATABASE IF NOT EXISTS poll CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'poll_springboot'@'localhost' IDENTIFIED WITH 'mysql_native_password' BY 'Spring2014';
CREATE USER IF NOT EXISTS 'poll_springboot'@'%' IDENTIFIED WITH 'mysql_native_password' BY 'Spring2014';

GRANT ALL PRIVILEGES ON poll.* TO 'poll_springboot'@'localhost';
GRANT ALL PRIVILEGES ON poll.* TO 'poll_springboot'@'%';

FLUSH PRIVILEGES;