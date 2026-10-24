import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Inventory {
	private ArrayList<Product> products = new ArrayList<>();
	private ArrayList<Supplier> suppliers = new ArrayList<>();
	private Map<String, String> supplierIds = new LinkedHashMap<>();
	private int lowStockLimit;
	private DatabaseManager databaseManager;

	public Inventory(int lowStockLimit) {
		setLowStockLimit(lowStockLimit);
	}

	public void setDatabaseManager(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
	}

	public DatabaseManager getDatabaseManager() {
		return databaseManager;
	}

	public int getLowStockLimit() {
		return lowStockLimit;
	}

	public void setLowStockLimit(int limit) {
		if (limit < 0) {
			throw new IllegalArgumentException("Limit can't be negative");
		}
		lowStockLimit = limit;
	}

	private int findProduct(String id) {
		for (int i = 0; i < products.size(); i++) {
			if (products.get(i).getProductId().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	private int findSupplier(String id) {
		for (int i = 0; i < suppliers.size(); i++) {
			if (suppliers.get(i).getId().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	public void addSupplier(Supplier s) {
		if (s == null) {
			throw new IllegalArgumentException("Supplier cannot be null");
		}
		if (findSupplier(s.getId()) != -1) {
			throw new IllegalArgumentException("Supplier ID already exists: " + s.getId());
		}
		if (databaseManager != null) {
			try {
				databaseManager.saveSupplier(s);
			} catch (SQLException e) {
				throw new IllegalStateException("Failed to save supplier to database: " + e.getMessage(), e);
			}
		}
		suppliers.add(s);
	}

	// Internal loader that bypasses DB save (used when loading from DB on startup)
	public void loadSupplierFromDb(Supplier s) {
		if (findSupplier(s.getId()) == -1) {
			suppliers.add(s);
		}
	}

	public Supplier getSupplier(String id) {
		int pos = findSupplier(id);
		if (pos == -1) {
			throw new IllegalArgumentException("No supplier with ID " + id);
		}
		return suppliers.get(pos);
	}

	public ArrayList<Supplier> getAllSuppliers() {
		return new ArrayList<>(suppliers);
	}

	public void addProduct(Product p, String supplierId) {
		if (p == null) {
			throw new IllegalArgumentException("Product cannot be null");
		}
		if (findProduct(p.getProductId()) != -1) {
			throw new IllegalArgumentException("Product ID already exists: " + p.getProductId());
		}
		if (!p.isValid()) {
			throw new IllegalArgumentException("Product is invalid: " + p.getProductId());
		}
		getSupplier(supplierId); // Verify supplier exists

		if (databaseManager != null) {
			try {
				databaseManager.saveProduct(p, supplierId);
			} catch (SQLException e) {
				throw new IllegalStateException("Failed to save product to database: " + e.getMessage(), e);
			}
		}

		products.add(p);
		supplierIds.put(p.getProductId(), supplierId);
	}

	// Internal loader that bypasses DB save (used when loading from DB on startup)
	public void loadProductFromDb(Product p, String supplierId) {
		if (findProduct(p.getProductId()) == -1) {
			products.add(p);
			supplierIds.put(p.getProductId(), supplierId);
		}
	}

	public Product getProduct(String id) {
		int pos = findProduct(id);
		if (pos == -1) {
			throw new IllegalArgumentException("No product with ID " + id);
		}
		return products.get(pos);
	}

	public ArrayList<Product> getAllProducts() {
		return new ArrayList<>(products);
	}

	public Supplier getSupplierOf(String productId) {
		int pos = findProduct(productId);
		if (pos == -1) {
			throw new IllegalArgumentException("No product with ID " + productId);
		}
		return getSupplier(supplierIds.get(productId));
	}

	public void updateProduct(String id, String name, double price, String supplierId) {
		int pos = findProduct(id);
		if (pos == -1) {
			throw new IllegalArgumentException("No product with ID " + id);
		}
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Product name can't be empty");
		}
		if (price <= 0) {
			throw new IllegalArgumentException("Price must be greater than 0");
		}
		getSupplier(supplierId);
		Product p = products.get(pos);
		p.setName(name);
		p.setPrice(price);
		supplierIds.put(id, supplierId);

		if (databaseManager != null) {
			try {
				databaseManager.saveProduct(p, supplierId);
			} catch (SQLException e) {
				throw new IllegalStateException("Failed to update product in database: " + e.getMessage(), e);
			}
		}
	}

	public void adjustStock(String id, int change) {
		Product p = getProduct(id);
		int newQty = p.getQuantity() + change;
		if (newQty < 0) {
			throw new IllegalArgumentException("Not enough stock for " + id);
		}
		p.setQuantity(newQty);
		if (databaseManager != null) {
			try {
				databaseManager.updateProductStock(id, newQty);
			} catch (SQLException e) {
				System.err.println("Database stock update failed for " + id + ": " + e.getMessage());
			}
		}
	}

	public void restock(String id, int qty) {
		if (qty <= 0) {
			throw new IllegalArgumentException("Restock quantity must be more than 0");
		}
		adjustStock(id, qty);
	}

	public boolean hasStock(String id, int qty) {
		return qty > 0 && getProduct(id).getQuantity() >= qty;
	}

	public void deductStock(String id, int qty) {
		if (qty <= 0) {
			throw new IllegalArgumentException("Quantity must be more than 0");
		}
		adjustStock(id, -qty);
	}

	public boolean isLowStock(String id) {
		return getProduct(id).getQuantity() <= lowStockLimit;
	}

	public ArrayList<Product> getLowStockProducts() {
		ArrayList<Product> low = new ArrayList<>();
		for (Product p : products) {
			if (p.getQuantity() <= lowStockLimit) {
				low.add(p);
			}
		}
		return low;
	}

	public void removeProduct(String id) {
		int pos = findProduct(id);
		if (pos == -1) {
			throw new IllegalArgumentException("No product with ID " + id);
		}
		if (databaseManager != null) {
			try {
				databaseManager.deleteProduct(id);
			} catch (SQLException e) {
				throw new IllegalStateException("Failed to delete product from database: " + e.getMessage(), e);
			}
		}
		products.remove(pos);
		supplierIds.remove(id);
	}

	public void removeSupplier(String id) {
		// Prevent removal if any product is still linked to this supplier
		for (String suppId : supplierIds.values()) {
			if (suppId.equals(id)) {
				throw new IllegalStateException("Cannot remove supplier " + id + ": products are still linked to it.");
			}
		}
		int pos = findSupplier(id);
		if (pos == -1) {
			throw new IllegalArgumentException("No supplier with ID " + id);
		}
		if (databaseManager != null) {
			try {
				databaseManager.deleteSupplier(id);
			} catch (SQLException e) {
				throw new IllegalStateException("Failed to delete supplier from database: " + e.getMessage(), e);
			}
		}
		suppliers.remove(pos);
	}

	// --- Expiry Queries ---

	public List<PerishableProduct> getExpiredProducts() {
		List<PerishableProduct> list = new ArrayList<>();
		for (Product p : products) {
			if (p instanceof PerishableProduct) {
				PerishableProduct per = (PerishableProduct) p;
				if (per.isExpired()) {
					list.add(per);
				}
			}
		}
		return list;
	}

	public List<PerishableProduct> getExpiringProducts(int days) {
		List<PerishableProduct> list = new ArrayList<>();
		for (Product p : products) {
			if (p instanceof PerishableProduct) {
				PerishableProduct per = (PerishableProduct) p;
				if (per.isExpiringSoon(days)) {
					list.add(per);
				}
			}
		}
		return list;
	}

	public String expiryReport() {
		StringBuilder report = new StringBuilder("=== Perishable Products Expiry Report ===\n\n");
		List<PerishableProduct> expired = getExpiredProducts();
		List<PerishableProduct> expiringSoon = getExpiringProducts(7);

		report.append("--- EXPIRED PRODUCTS (Action: Remove from shelves!) ---\n");
		if (expired.isEmpty()) {
			report.append("None. All perishable items are within date.\n");
		} else {
			report.append(String.format("%-8s %-20s %-12s %6s %s\n", "ID", "Name", "Expiry Date", "Stock", "Supplier"));
			for (PerishableProduct p : expired) {
				Supplier s = getSupplier(supplierIds.get(p.getProductId()));
				report.append(String.format("%-8s %-20s %-12s %6d %s\n",
						p.getProductId(), p.getName(), p.getExpiryDate(), p.getQuantity(), s.getName()));
			}
		}

		report.append("\n--- EXPIRING SOON (Within 7 Days) ---\n");
		if (expiringSoon.isEmpty()) {
			report.append("None.\n");
		} else {
			report.append(String.format("%-8s %-20s %-12s %-10s %6s %s\n", "ID", "Name", "Expiry Date", "Days Left", "Stock", "Supplier"));
			for (PerishableProduct p : expiringSoon) {
				Supplier s = getSupplier(supplierIds.get(p.getProductId()));
				report.append(String.format("%-8s %-20s %-12s %-10d %6d %s\n",
						p.getProductId(), p.getName(), p.getExpiryDate(), p.getDaysUntilExpiry(), p.getQuantity(), s.getName()));
			}
		}

		return report.toString();
	}

	public String stockReport() {
		StringBuilder report = new StringBuilder(
			String.format("%-8s %-20s %-14s %6s %10s %-22s %s\n", "ID", "Name", "Type", "Qty", "Price", "Details", "Supplier"));
		double total = 0;
		for (int i = 0; i < products.size(); i++) {
			Product p = products.get(i);
			Supplier s = getSupplier(supplierIds.get(p.getProductId()));
			String details = "";
			if (p instanceof PerishableProduct) {
				PerishableProduct per = (PerishableProduct) p;
				if (per.isExpired()) {
					details = "Exp: " + per.getExpiryDate() + " [EXPIRED]";
				} else if (per.isExpiringSoon(7)) {
					details = "Exp: " + per.getExpiryDate() + " [" + per.getDaysUntilExpiry() + "d left]";
				} else {
					details = "Exp: " + per.getExpiryDate();
				}
			} else if (p instanceof NonPerishableProduct) {
				details = "Warranty: " + ((NonPerishableProduct) p).getWarrantyMonths() + " mo";
			}

			report.append(String.format("%-8s %-20s %-14s %6d %10.2f %-22s %s\n",
					p.getProductId(), p.getName(), p.getProductType(), p.getQuantity(), p.getPrice(), details, s.getName()));
			total += p.getQuantity() * p.getPrice();
		}
		report.append("----------------------------------------------------------------------------------------------------\n");
		report.append("Total products: ").append(products.size()).append("\n");
		report.append(String.format("Total stock value: ₹%.2f\n", total));
		return report.toString();
	}

	public String lowStockReport() {
		StringBuilder report = new StringBuilder("Low stock items (limit " + lowStockLimit + ")\n");
		boolean anyLow = false;
		for (int i = 0; i < products.size(); i++) {
			Product p = products.get(i);
			if (p.getQuantity() <= lowStockLimit) {
				Supplier s = getSupplier(supplierIds.get(p.getProductId()));
				report.append(String.format("%-8s %-20s qty %-4d reorder from %s (%s)\n",
						p.getProductId(), p.getName(), p.getQuantity(), s.getName(), s.getPhone()));
				anyLow = true;
			}
		}
		if (!anyLow) {
			report.append("Nothing is running low.\n");
		}
		return report.toString();
	}
}