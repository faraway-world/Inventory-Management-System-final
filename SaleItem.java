public class SaleItem {
    private Product product;
    private int quantity;
    private double subtotal;

    public SaleItem(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        this.product = product;
        this.quantity = quantity;
        this.subtotal = product.getPrice() * quantity;
    }

    public double getSubtotal() { return subtotal; }
    public int getQuantity() { return quantity; }
    public Product getProduct() { return product; }
}