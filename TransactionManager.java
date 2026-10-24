import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;

public class TransactionManager {
    private List<Sale> transactionHistory;
    private Inventory inventory;
    private DatabaseManager databaseManager;

    public TransactionManager(Inventory inventory) {
        this(inventory, null);
    }

    public TransactionManager(Inventory inventory, DatabaseManager databaseManager) {
        this.transactionHistory = new ArrayList<>();
        this.inventory = inventory;
        this.databaseManager = databaseManager;
    }

    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void loadTransactionHistoryFromDb(List<Sale> sales) {
        transactionHistory.clear();
        if (sales != null) {
            transactionHistory.addAll(sales);
        }
    }

    public boolean hasSaleId(String saleId) {
        if (saleId == null) {
            return false;
        }
        for (Sale s : transactionHistory) {
            if (s.getSaleId().equalsIgnoreCase(saleId.trim())) {
                return true;
            }
        }
        return false;
    }

    public List<Sale> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);
    }

    public boolean processSale(Sale sale) {
        if (sale == null) {
            throw new IllegalArgumentException("Sale cannot be null");
        }
        if (hasSaleId(sale.getSaleId())) {
            throw new IllegalArgumentException("Sale ID already exists: " + sale.getSaleId());
        }

        // 1. Verify that no perishable item being sold has expired!
        for (SaleItem item : sale.getItems()) {
            Product p = item.getProduct();
            if (p instanceof PerishableProduct) {
                PerishableProduct per = (PerishableProduct) p;
                if (per.isExpired()) {
                    throw new IllegalStateException("Cannot sell expired product: " + per.getName()
                            + " (Expired on " + per.getExpiryDate() + "). Please dispose or return this item.");
                }
            }
        }

        // 2. Verify stock availability
        for (SaleItem item : sale.getItems()) {
            if (!inventory.hasStock(item.getProduct().getProductId(), item.getQuantity())) {
                throw new IllegalStateException("Insufficient stock for: " + item.getProduct().getName()
                        + " (Requested: " + item.getQuantity() + ", Available: "
                        + inventory.getProduct(item.getProduct().getProductId()).getQuantity() + ")");
            }
        }

        // 3. Deduct stock and persist to DB
        List<SaleItem> deductedItems = new ArrayList<>();
        try {
            for (SaleItem item : sale.getItems()) {
                inventory.deductStock(item.getProduct().getProductId(), item.getQuantity());
                deductedItems.add(item);
            }
            if (databaseManager != null) {
                databaseManager.saveSale(sale);
            }
        } catch (SQLException | RuntimeException exception) {
            // Roll back in-memory deductions if save fails
            for (SaleItem item : deductedItems) {
                inventory.adjustStock(item.getProduct().getProductId(), item.getQuantity());
            }
            throw new IllegalStateException("Sale could not be saved to database: " + exception.getMessage(), exception);
        }

        transactionHistory.add(sale);
        return true;
    }

    public String salesReport() {
        StringBuilder report = new StringBuilder("=== Sales Transaction History ===\n\n");
        report.append(String.format("%-10s %-22s %-12s %s\n", "Sale ID", "Date", "Total", "Items"));
        report.append("----------------------------------------------------------------------------------------------------\n");
        if (transactionHistory.isEmpty()) {
            report.append("No sales recorded yet.\n");
            return report.toString();
        }
        double grandTotal = 0;
        for (Sale s : transactionHistory) {
            StringBuilder itemsDesc = new StringBuilder();
            for (SaleItem item : s.getItems()) {
                if (itemsDesc.length() > 0) itemsDesc.append(", ");
                itemsDesc.append(item.getProduct().getName()).append(" x").append(item.getQuantity());
            }
            report.append(String.format("%-10s %-22s ₹%-11.2f %s\n",
                    s.getSaleId(), s.getSaleDate().toString(), s.getTotalAmount(), itemsDesc.toString()));
            grandTotal += s.getTotalAmount();
        }
        report.append("----------------------------------------------------------------------------------------------------\n");
        report.append("Total Transactions: ").append(transactionHistory.size()).append("\n");
        report.append(String.format("Grand Total Revenue: ₹%.2f\n", grandTotal));
        return report.toString();
    }
}
