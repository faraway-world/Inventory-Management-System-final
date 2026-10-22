public class NonPerishableProduct extends Product {
    private int warrantyMonths;

    public NonPerishableProduct(String productId, String name, double price, int quantity, int warrantyMonths) {
        super(productId, name, price, quantity);
        this.warrantyMonths = (warrantyMonths >= 0) ? warrantyMonths : 0;
    }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    @Override
    public String getProductType() {
        return "Non-Perishable";
    }

    @Override
    public void displayDetails() {
        System.out.println("ID: " + getProductId() + " | Name: " + getName() + " | Type: Non-Perishable | Price: ₹" + getPrice() + " | Stock: " + getQuantity() + " | Warranty: " + warrantyMonths + " months");
    }
}

