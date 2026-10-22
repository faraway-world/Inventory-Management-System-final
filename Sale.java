import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class Sale {
    private String saleId;
    private Date saleDate;
    private List<SaleItem> items; // Holds all items in this transaction
    private double totalAmount;

    public Sale(String saleId) {
        if (saleId == null || saleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Sale ID cannot be empty");
        }
        this.saleId = saleId.trim();
        this.saleDate = new Date();
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
    }

    // Add an item to the bill and update the total
    public void addSaleItem(SaleItem item) {
        items.add(item);
        totalAmount += item.getSubtotal(); // Add item's subtotal to grand total
    }

    public double getTotalAmount() { return totalAmount; }
    public String getSaleId() { return saleId; }
    public Date getSaleDate() { return saleDate; }
    public List<SaleItem> getItems() { return items; }
}
