CREATE DATABASE IF NOT EXISTS hhplus;
USE hhplus;
DROP USER IF EXISTS 'application'@'%%';
DROP USER IF EXISTS 'application'@'localhost';
CREATE USER 'application'@'%%' IDENTIFIED WITH mysql_native_password BY 'application';
CREATE USER 'application'@'localhost' IDENTIFIED WITH mysql_native_password BY 'application';
GRANT ALL PRIVILEGES ON hhplus.* TO 'application'@'%%';
GRANT ALL PRIVILEGES ON hhplus.* TO 'application'@'localhost';
FLUSH PRIVILEGES;