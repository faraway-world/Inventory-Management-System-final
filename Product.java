
public abstract class Product {
    private String productId;
    private String name;
    private double price;
    private int quantity;

    public Product(String productId, String name, double price, int quantity) {
        this.productId = productId;
        this.name = name;
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.price = price;
        this.quantity = quantity;
    }

    public abstract String getProductType();
    public abstract void displayDetails();

    public boolean isValid() {
        if (productId == null || productId.trim().isEmpty()) {
            System.out.println("Validation Error: Product ID cannot be empty.");
            return false;
        }
        if (name == null || name.trim().isEmpty()) {
            System.out.println("Validation Error: Product name cannot be empty.");
            return false;
        }
        if (price <= 0) {
            System.out.println("Validation Error: Price must be greater than 0 for " + name);
            return false;
        }
        if (quantity < 0) {
            System.out.println("Validation Error: Quantity cannot be negative for " + name);
            return false;
        }
        return true;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) {
        if (price > 0) {
            this.price = price;
        } else {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
    }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        if (quantity >= 0) {
            this.quantity = quantity;
        } else {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
}

