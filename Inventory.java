import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Inventory {
	private ArrayList<Product> products = new ArrayList<>();
	private ArrayList<Supplier> suppliers = new ArrayList<>();
	private Map<String, String> supplierIds = new LinkedHashMap<>();
	private int lowStockLimit;

	public Inventory(int lowStockLimit) {
		setLowStockLimit(lowStockLimit);
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
		if (findSupplier(s.getId()) != -1) {
			throw new IllegalArgumentException("Supplier ID already exists: " + s.getId());
		}
		suppliers.add(s);
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
		getSupplier(supplierId);
		products.add(p);
		supplierIds.put(p.getProductId(), supplierId);
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
		if (name == null || name.trim().equals("")) {
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
	}

	public void adjustStock(String id, int change) {
		Product p = getProduct(id);
		int newQty = p.getQuantity() + change;
		if (newQty < 0) {
			throw new IllegalArgumentException("Not enough stock for " + id);
		}
		p.setQuantity(newQty);
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
		suppliers.remove(pos);
	}

	public String stockReport() {
		StringBuilder report = new StringBuilder(
			String.format("%-8s %-20s %6s %10s %s\n", "ID", "Name", "Qty", "Price", "Supplier"));
		double total = 0;
		for (int i = 0; i < products.size(); i++) {
			Product p = products.get(i);
			Supplier s = getSupplier(supplierIds.get(p.getProductId()));
			report.append(String.format("%-8s %-20s %6d %10.2f %s\n",
					p.getProductId(), p.getName(), p.getQuantity(), p.getPrice(), s.getName()));
			total += p.getQuantity() * p.getPrice();
		}
		report.append("Total products: ").append(products.size()).append("\n");
		report.append(String.format("Total stock value: %.2f\n", total));
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