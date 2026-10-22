public class PerishableProduct extends Product {
    private String expiryDate;

    public PerishableProduct(String productId, String name, double price, int quantity, String expiryDate) {
        super(productId, name, price, quantity);
        this.expiryDate = expiryDate;
    }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            System.out.println("Validation Error: Expiry date is required for perishable product " + getName());
            return false;
        }
        return true;
    }

    @Override
    public String getProductType() {
        return "Perishable";
    }

    @Override
    public void displayDetails() {
        System.out.println("ID: " + getProductId() + " | Name: " + getName() + 
                   " | Type: Perishable | Price: ₹" + getPrice() + 
                           " | Stock: " + getQuantity() + " | Expiry: " + expiryDate);
    }
}