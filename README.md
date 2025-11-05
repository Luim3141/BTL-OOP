In IntelliJ IDEA:
Right-click the folder "src" → Mark Directory As → Resources Root

# 📚 Library Management System (BTL-OOP)

A Java Swing-based library management system built with **NetBeans / IntelliJ IDEA**, connected to a **MySQL** database.

---

## ⚙️ Requirements

1. **Java JDK 17 or higher**
2. **MySQL Server**
3. **MySQL Connector/J 8.3.0**

---

## 🧩 Database Setup(optional)

1. Create a MySQL database named `library`.
2. Import the required tables manually from your project (e.g., `book`, `student`, `login`, `request`, etc.).
3. Update your database connection details inside:


## ⚙️ Setup Instructions
1. Import `database/library.sql` into MySQL.
2. Open project in IntelliJ or NetBeans.
3. Add `lib/mysql-connector-j-8.3.0.jar` to project libraries.
4. Run `SignIn.java`

##  Create database Instructions
-- ============================================
-- 📚 DATABASE: library
-- ============================================
CREATE DATABASE IF NOT EXISTS library;
USE library;

-- ============================================
-- 1️⃣ TABLE: login  (quản lý tài khoản đăng nhập)
-- ============================================
CREATE TABLE IF NOT EXISTS login (
userid VARCHAR(50) PRIMARY KEY,
password VARCHAR(100) NOT NULL,
role ENUM('admin', 'client') NOT NULL
);

-- ✅ Dữ liệu mẫu
INSERT INTO login (userid, password, role) VALUES
('admin', 'admin123', 'admin'),
('client1', '12345', 'client'),
('client2', 'abcde', 'client');

-- ============================================
-- 2️⃣ TABLE: student (quản lý thông tin sinh viên)
-- ============================================
CREATE TABLE IF NOT EXISTS student (
id INT AUTO_INCREMENT PRIMARY KEY,
name VARCHAR(100) NOT NULL,
email VARCHAR(100),
phone VARCHAR(20),
address VARCHAR(255)
);

-- ✅ Dữ liệu mẫu
INSERT INTO student (name, email, phone, address) VALUES
('Nguyen Van A', 'a@gmail.com', '0912345678', 'Hanoi'),
('Tran Thi B', 'b@gmail.com', '0987654321', 'Ho Chi Minh City'),
('Le Van C', 'c@gmail.com', '0909090909', 'Da Nang');

-- ============================================
-- 3️⃣ TABLE: book (quản lý sách)
-- ============================================
CREATE TABLE IF NOT EXISTS book (
id VARCHAR(10) PRIMARY KEY,
name VARCHAR(100) NOT NULL,
author VARCHAR(100),
publisher VARCHAR(100),
price DECIMAL(10,2),
year INT,
status ENUM('Issued', 'NotIssued') DEFAULT 'NotIssued',
issue_date DATE DEFAULT NULL,
due_date DATE DEFAULT NULL,
studentid INT DEFAULT NULL,
FOREIGN KEY (studentid) REFERENCES student(id) ON DELETE SET NULL
);

-- ✅ Dữ liệu mẫu
INSERT INTO book (id, name, author, publisher, price, year, status) VALUES
('A1', 'To Kill a Mockingbird', 'Harper Lee', 'HarperCollins', 150.00, 2009, 'NotIssued'),
('B1', '1984', 'George Orwell', 'Penguin', 120.00, 2020, 'NotIssued'),
('C1', 'The Great Gatsby', 'F. Scott Fitzgerald', 'Scribner', 180.00, 2004, 'Issued');

-- ============================================
-- 4️⃣ TABLE: request (yêu cầu mượn sách từ client)
-- ============================================
CREATE TABLE IF NOT EXISTS request (
id INT AUTO_INCREMENT PRIMARY KEY,
book_id VARCHAR(10) NOT NULL,
book_name VARCHAR(100) NOT NULL,
student_id INT NOT NULL,
student_name VARCHAR(100) NOT NULL,
issue_date DATE DEFAULT NULL,
due_date DATE DEFAULT NULL,
FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE,
FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
);

## 📂 Project Structure
- `src/system`: All source code (UI + Logic)
- `img/`: Application icons and background
- `lib/`: MySQL JDBC Driver
- `database/`: SQL dump of the database