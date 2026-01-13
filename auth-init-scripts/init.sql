CREATE DATABASE IF NOT EXISTS user;

CREATE USER 'everyauth_springboot'@'%' IDENTIFIED BY 'AuthSpring2014';

GRANT ALL PRIVILEGES ON user.* TO 'everyauth_springboot'@'%';

FLUSH PRIVILEGES;
