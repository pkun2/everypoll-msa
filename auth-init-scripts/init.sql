CREATE DATABASE IF NOT EXISTS user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'auth_springboot'@'localhost' IDENTIFIED WITH 'mysql_native_password' BY 'AuthSpring2014';
CREATE USER IF NOT EXISTS 'auth_springboot'@'%' IDENTIFIED WITH 'mysql_native_password' BY 'AuthSpring2014';

GRANT ALL PRIVILEGES ON user.* TO 'auth_springboot'@'localhost';
GRANT ALL PRIVILEGES ON user.* TO 'auth_springboot'@'%';

FLUSH PRIVILEGES;
