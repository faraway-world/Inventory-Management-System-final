# Inventory Management System

A desktop-based retail inventory and sales management application built using **Java (Swing GUI)** with object-oriented design and **MySQL** database persistence.

The system features product classification (Perishable vs. Non-Perishable), supplier tracking, stock adjustments, transaction billing with rollback support, automated low-stock alerts, and purchase reorder workflows.

---

## 📌 Features

* **Product Hierarchy (OOP)**:
  * Abstract base `Product` class enforcing validation, price, and stock integrity.
  * `PerishableProduct` tracking expiry dates.
  * `NonPerishableProduct` tracking warranty periods.
* **Supplier Management**: Register and link suppliers to inventory products with contact details.
* **Search Module**: Real-time case-insensitive keyword search by product name.
* **Stock Operations**: Quick restock and manual stock deduction with threshold warnings.
* **Sales & Billing**:
  * Calculates bill amounts with stock availability checks.
  * Multi-item support with transactional integrity (in-memory rollback if database save fails).
  * Sales history view tracking all past transactions and revenue.
* **Automated Low-Stock Alert & Reordering**:
  * Monitors inventory levels against configurable threshold limits.
  * Automatically calculates required units (`targetStock - currentStock`) and generates pending reorder requests (`RO1`, `RO2`, etc.).
  * Notifies the user via alerts and console logs.
  * Receiving shipment updates inventory stock automatically.
* **Dual Execution Mode**:
  * **With Database**: Automatically connects to MySQL and logs sales to SQL tables.
  * **In-Memory Fallback**: If MySQL is offline, the system continues running smoothly in-memory without crashing.

---

## 🏛️ OOP Concepts Demonstrated

| OOP Concept | Implementation Details |
| :--- | :--- |
| **Abstraction** | `Product` is an `abstract` class defining abstract methods `getProductType()` and `displayDetails()`. |
| **Inheritance** | `PerishableProduct` and `NonPerishableProduct` extend `Product` via `super(...)` constructors. |
| **Polymorphism** | Overriding (`@Override`) `displayDetails()`, `getProductType()`, and `isValid()` across subclasses. |
| **Encapsulation** | Private fields (`id`, `price`, `quantity`, etc.) protected with validated getters and setters. |
| **Interfaces** | `AlertNotifier` interface decoupling reorder notification logic from the UI. |
| **Collections Framework** | `ArrayList` for dynamic lists; `LinkedHashMap` for mapping product IDs to supplier IDs. |
| **Exception Handling** | Validation via `IllegalArgumentException`, `IllegalStateException`, and transactional `SQLException`. |

---

## 🛠️ Tech Stack & Requirements

* **Language**: Java 17 or higher (tested on Java 21 / 27)
* **GUI Framework**: Java Swing (`JFrame`, `JTabbedPane`, `GridLayout`, `BorderLayout`)
* **Database**: MySQL / MariaDB (Driver `mysql-connector-j-26.10.0.jar` included)
* **OS**: Cross-platform (Linux, Windows, macOS)

---

## 🚀 Quick Start & How to Run

### 1. Run Directly (No Setup Needed)
The project comes with the JDBC driver and a launcher script ready to run:

```bash
# Clone the repository
git clone https://github.com/faraway-world/Inventory-Management-System-final.git
cd Inventory-Management-System-final

# Run the application
chmod +x run.sh
./run.sh
```

*(Or compile and run manually using Java):*
```bash
javac -cp ".:*" *.java
java -cp ".:*" Main
```

The system will start up in **in-memory mode** by default if MySQL is not running.

---

### 2. Optional: Enable MySQL Database
To persist sales transactions into MySQL:

1. Run the included setup script:
   ```bash
   sudo ./setup_mysql.sh
   ```
2. Or configure manually in MySQL:
   ```sql
   CREATE DATABASE oop_inventory;
   GRANT ALL PRIVILEGES ON oop_inventory.* TO 'root'@'localhost' IDENTIFIED BY '';
   FLUSH PRIVILEGES;
   ```
3. Launch the app (`./run.sh`). It will automatically connect and log:
   ```text
   Connected to MySQL database.
   [System] MySQL connected
   ```

---

## 📁 Project Structure

```text
.
├── AlertNotifier.java               # Interface for decoupled low-stock notification
├── DatabaseConnection.java          # JDBC connection provider with environment variable overrides
├── DatabaseManager.java             # Schema creation and transactional sale persistence
├── Inventory.java                   # In-memory product/supplier repository & report generator
├── Main.java                        # Swing graphical user interface and event orchestration
├── mysql-connector-j-26.10.0.jar    # Bundled MySQL JDBC driver
├── NonPerishableProduct.java        # Product subclass with warranty support
├── PerishableProduct.java           # Product subclass with expiry date support
├── Product.java                     # Abstract base entity with validation logic
├── ProductSearch.java               # Case-insensitive substring search helper
├── README.md                        # Documentation
├── ReorderManager.java              # Automated reorder engine & threshold tracking
├── ReorderRequest.java              # Reorder record entity with status lifecycle
├── ReorderStatus.java               # Enum (PENDING, RECEIVED)
├── run.sh                           # One-click compile & launch script
├── Sale.java                        # Sales header entity tracking ID, date, and line items
├── SaleItem.java                    # Line item entity connecting product, quantity, and subtotal
├── setup_mysql.sh                   # MariaDB/MySQL auto-configuration script
├── Supplier.java                    # Supplier entity with contact info
└── TransactionManager.java          # Business logic coordinating stock deduction & database sync
```

---

## 🗄️ Database Schema

The database tables are automatically initialized on the first connection:

* **`sales`**:
  * `sale_id` (VARCHAR(100), Primary Key)
  * `sale_date` (TIMESTAMP)
  * `total_amount` (DECIMAL(12, 2))
* **`sale_items`**:
  * `id` (BIGINT, Primary Key AUTO_INCREMENT)
  * `sale_id` (VARCHAR(100), Foreign Key -> `sales.sale_id`)
  * `product_id` (VARCHAR(100))
  * `quantity` (INT)
  * `subtotal` (DECIMAL(12, 2))

---

## 📜 License
This project is developed for educational and academic presentation purposes.
