import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final DatabaseConnection databaseConnection;

    public DatabaseManager() {
        this.databaseConnection = new DatabaseConnection();
        initializeSchema();
    }

    private void initializeSchema() {
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
                + "FOREIGN KEY (sale_id) REFERENCES sales(sale_id))";

        try (Connection connection = databaseConnection.open();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(salesTable);
            statement.executeUpdate(itemsTable);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize the MySQL schema", exception);
        }
    }

    public void saveSale(Sale sale) throws SQLException {
        String saleSql = "INSERT INTO sales (sale_id, sale_date, total_amount) VALUES (?, ?, ?)";
        String itemSql = "INSERT INTO sale_items (sale_id, product_id, quantity, subtotal) VALUES (?, ?, ?, ?)";

        try (Connection connection = databaseConnection.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement saleStatement = connection.prepareStatement(saleSql);
                 PreparedStatement itemStatement = connection.prepareStatement(itemSql)) {
                saleStatement.setString(1, sale.getSaleId());
                saleStatement.setTimestamp(2, new java.sql.Timestamp(sale.getSaleDate().getTime()));
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
    //As of rn, only sale data is stored in MySQL. Other data can be added during extension time if needed.
}