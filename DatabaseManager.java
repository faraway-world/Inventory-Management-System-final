import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    public static class ProductRecord {
        private final Product product;
        private final String supplierId;

        public ProductRecord(Product product, String supplierId) {
            this.product = product;
            this.supplierId = supplierId;
        }

        public Product getProduct() { return product; }
        public String getSupplierId() { return supplierId; }
    }

    private final DatabaseConnection databaseConnection;

    public DatabaseManager() {
        this(new DatabaseConnection());
    }

    public DatabaseManager(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        initializeSchema();
    }

    private void initializeSchema() {
        String suppliersTable = "CREATE TABLE IF NOT EXISTS suppliers ("
                + "supplier_id VARCHAR(100) PRIMARY KEY, "
                + "name VARCHAR(255) NOT NULL, "
                + "phone VARCHAR(50), "
                + "email VARCHAR(100))";

        String productsTable = "CREATE TABLE IF NOT EXISTS products ("
                + "product_id VARCHAR(100) PRIMARY KEY, "
                + "name VARCHAR(255) NOT NULL, "
                + "price DECIMAL(12, 2) NOT NULL, "
                + "quantity INT NOT NULL, "
                + "product_type VARCHAR(50) NOT NULL, "
                + "expiry_date VARCHAR(50), "
                + "warranty_months INT, "
                + "supplier_id VARCHAR(100) NOT NULL, "
                + "FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id) ON UPDATE CASCADE)";

        String salesTable = "CREATE TABLE IF NOT EXISTS sales ("
                + "sale_id VARCHAR(100) PRIMARY KEY, "
                + "sale_date TIMESTAMP NOT NULL, "
                + "total_amount DECIMAL(12, 2) NOT NULL)";

        String itemsTable = "CREATE TABLE IF NOT EXISTS sale_items ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "sale_id VARCHAR(100) NOT NULL, "
                + "product_id VARCHAR(100) NOT NULL, "
                + "quantity INT NOT NULL, "
                + "subtotal DECIMAL(12, 2) NOT NULL, "
                + "FOREIGN KEY (sale_id) REFERENCES sales(sale_id) ON DELETE CASCADE)";

        String reorderTable = "CREATE TABLE IF NOT EXISTS reorder_requests ("
                + "request_id VARCHAR(100) PRIMARY KEY, "
                + "product_id VARCHAR(100) NOT NULL, "
                + "supplier_id VARCHAR(100) NOT NULL, "
                + "quantity INT NOT NULL, "
                + "request_date TIMESTAMP NOT NULL, "
                + "received_date TIMESTAMP NULL, "
                + "status VARCHAR(20) NOT NULL)";

        try (Connection connection = databaseConnection.open();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(suppliersTable);
            statement.executeUpdate(productsTable);
            statement.executeUpdate(salesTable);
            statement.executeUpdate(itemsTable);
            statement.executeUpdate(reorderTable);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize the MySQL schema: " + exception.getMessage(), exception);
        }
    }

    // --- Suppliers Persistence ---

    public List<Supplier> loadSuppliers() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT supplier_id, name, phone, email FROM suppliers ORDER BY supplier_id";
        try (Connection connection = databaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Supplier s = new Supplier(
                        rs.getString("supplier_id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email")
                );
                list.add(s);
            }
        }
        return list;
    }

    public void saveSupplier(Supplier s) throws SQLException {
        String sql = "INSERT INTO suppliers (supplier_id, name, phone, email) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE name = VALUES(name), phone = VALUES(phone), email = VALUES(email)";
        try (Connection connection = databaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, s.getId());
            statement.setString(2, s.getName());
            statement.setString(3, s.getPhone());
            statement.setString(4, s.getEmail());
            statement.executeUpdate();
        }
    }

    public void deleteSupplier(String supplierId) throws SQLException {
        String sql = "DELETE FROM suppliers WHERE supplier_id = ?";
        try (Connection connection = databaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, supplierId);
            statement.executeUpdate();
        }
    }

    // --- Products Persistence ---

    public List<ProductRecord> loadProducts() throws SQLException {
        List<ProductRecord> list = new ArrayList<>();
        String sql = "SELECT product_id, name, price, quantity, product_type, expiry_date, warranty_months, supplier_id "
                + "FROM products ORDER BY product_id";
        try (Connection connection = databaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("product_id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int qty = rs.getInt("quantity");
                String type = rs.getString("product_type");
                String expiry = rs.getString("expiry_date");
                int warranty = rs.getInt("warranty_months");
                String supplierId = rs.getString("supplier_id");

                Product product;
                if ("Perishable".equalsIgnoreCase(type)) {
                    product = new PerishableProduct(id, name, price, qty, expiry);
                } else {
                    product = new NonPerishableProduct(id, name, price, qty, warranty);
                }
                list.add(new ProductRecord(product, supplierId));
            }
        }
        return list;
    }

    public void saveProduct(Product p, String supplierId) throws SQLException {
        String sql = "INSERT INTO products (product_id, name, price, quantity, product_type, expiry_date, warranty_months, supplier_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE name = VALUES(name), price = VALUES(price), quantity = VALUES(quantity), "
                + "product_type = VALUES(product_type), expiry_date = VALUES(expiry_date), warranty_months = VALUES(warranty_months), "
                + "supplier_id = VALUES(supplier_id)";
        try (Connection connection = databaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, p.getProductId());
            statement.setString(2, p.getName());
            statement.setDouble(3, p.getPrice());
            statement.setInt(4, p.getQuantity());
            statement.setString(5, p.getProductType());
            if (p instanceof PerishableProduct) {
                statement.setString(6, ((PerishableProduct) p).getExpiryDate());
                statement.setNull(7, Types.INTEGER);
            } else if (p instanceof NonPerishableProduct) {
                statement.setNull(6, Types.VARCHAR);
                statement.setInt(7, ((NonPerishableProduct) p).getWarrantyMonths());
            } else {
                statement.setNull(6, Types.VARCHAR);
                statement.setNull(7, Types.INTEGER);
            }
            statement.setString(8, supplierId);
            statement.executeUpdate();
        }
    }

    public void updateProductStock(String productId, int newQuantity) throws SQLException {
        String sql = "UPDATE products SET quantity = ? WHERE product_id = ?";
        try (Connection connection = databaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, newQuantity);
            statement.setString(2, productId);
            statement.executeUpdate();
        }
    }

    public void deleteProduct(String productId) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection connection = databaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productId);
            statement.executeUpdate();
        }
    }

    // --- Sales Persistence ---

    public void saveSale(Sale sale) throws SQLException {
        String saleSql = "INSERT INTO sales (sale_id, sale_date, total_amount) VALUES (?, ?, ?)";
        String itemSql = "INSERT INTO sale_items (sale_id, product_id, quantity, subtotal) VALUES (?, ?, ?, ?)";

        try (Connection connection = databaseConnection.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement saleStatement = connection.prepareStatement(saleSql);
                 PreparedStatement itemStatement = connection.prepareStatement(itemSql)) {
                saleStatement.setString(1, sale.getSaleId());
                saleStatement.setTimestamp(2, new Timestamp(sale.getSaleDate().getTime()));
                saleStatement.setDouble(3, sale.getTotalAmount());
                saleStatement.executeUpdate();

                for (SaleItem item : sale.getItems()) {
                    itemStatement.setString(1, sale.getSaleId());
                    itemStatement.setString(2, item.getProduct().getProductId());
                    itemStatement.setInt(3, item.getQuantity());
                    itemStatement.setDouble(4, item.getSubtotal());
                    itemStatement.addBatch();
                }
                itemStatement.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Sale> loadSales(Inventory inventory) throws SQLException {
        Map<String, Sale> saleMap = new LinkedHashMap<>();
        String sql = "SELECT s.sale_id, s.sale_date, s.total_amount, "
                + "si.product_id, si.quantity, si.subtotal "
                + "FROM sales s "
                + "LEFT JOIN sale_items si ON s.sale_id = si.sale_id "
                + "ORDER BY s.sale_date ASC, si.id ASC";

        try (Connection connection = databaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String saleId = rs.getString("sale_id");
                Date saleDate = rs.getTimestamp("sale_date");
                Sale sale = saleMap.get(saleId);
                if (sale == null) {
                    sale = new Sale(saleId, saleDate);
                    saleMap.put(saleId, sale);
                }

                String productId = rs.getString("product_id");
                if (productId != null) {
                    int qty = rs.getInt("quantity");
                    Product prod = null;
                    if (inventory != null) {
                        try {
                            prod = inventory.getProduct(productId);
                        } catch (Exception ignored) {}
                    }
                    if (prod == null) {
                        double subtotal = rs.getDouble("subtotal");
                        double unitPrice = qty > 0 ? subtotal / qty : 0;
                        prod = new NonPerishableProduct(productId, "Product #" + productId, unitPrice, 0, 0);
                    }
                    sale.addSaleItem(new SaleItem(prod, qty));
                }
            }
        }
        return new ArrayList<>(saleMap.values());
    }

    // --- Reorder Requests Persistence ---

    public List<ReorderRequest> loadReorderRequests(Inventory inventory) throws SQLException {
        List<ReorderRequest> list = new ArrayList<>();
        String sql = "SELECT request_id, product_id, supplier_id, quantity, request_date, received_date, status "
                + "FROM reorder_requests ORDER BY request_date ASC";
        try (Connection connection = databaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String reqId = rs.getString("request_id");
                String prodId = rs.getString("product_id");
                String suppId = rs.getString("supplier_id");
                int qty = rs.getInt("quantity");
                Date reqDate = rs.getTimestamp("request_date");
                Date recDate = rs.getTimestamp("received_date");
                String statusStr = rs.getString("status");

                Product p = null;
                Supplier s = null;
                try {
                    p = inventory.getProduct(prodId);
                } catch (Exception ignored) {}
                try {
                    s = inventory.getSupplier(suppId);
                } catch (Exception ignored) {}

                if (p != null && s != null) {
                    ReorderRequest req = new ReorderRequest(reqId, p, s, qty, reqDate, recDate,
                            ReorderStatus.valueOf(statusStr));
                    list.add(req);
                }
            }
        }
        return list;
    }

    public void saveReorderRequest(ReorderRequest request) throws SQLException {
        String sql = "INSERT INTO reorder_requests (request_id, product_id, supplier_id, quantity, request_date, received_date, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE received_date = VALUES(received_date), status = VALUES(status)";
        try (Connection connection = databaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.getRequestId());
            statement.setString(2, request.getProduct().getProductId());
            statement.setString(3, request.getSupplier().getId());
            statement.setInt(4, request.getQuantity());
            statement.setTimestamp(5, new Timestamp(request.getRequestDate().getTime()));
            if (request.getReceivedDate() != null) {
                statement.setTimestamp(6, new Timestamp(request.getReceivedDate().getTime()));
            } else {
                statement.setNull(6, Types.TIMESTAMP);
            }
            statement.setString(7, request.getStatus().name());
            statement.executeUpdate();
        }
    }

    public void updateReorderRequest(ReorderRequest request) throws SQLException {
        saveReorderRequest(request);
    }
}