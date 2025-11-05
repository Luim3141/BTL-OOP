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

## 📂 Project Structure
- `src/system`: All source code (UI + Logic)
- `img/`: Application icons and background
- `lib/`: MySQL JDBC Driver
- `database/`: SQL dump of the database