import java.util.Date;

public class ReorderRequest {
    private String requestId;
    private Product product;
    private Supplier supplier;
    private int quantity;
    private Date requestDate;
    private Date receivedDate;
    private ReorderStatus status;

    public ReorderRequest(String requestId, Product product, Supplier supplier, int quantity) {
        if (product == null || supplier == null) {
            throw new IllegalArgumentException("Product and supplier are required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reorder quantity must be greater than 0");
        }
        this.requestId = requestId;
        this.product = product;
        this.supplier = supplier;
        this.quantity = quantity;
        this.requestDate = new Date();
        this.status = ReorderStatus.PENDING;
    }

    public boolean isPending() {
        return status == ReorderStatus.PENDING;
    }

    public void markReceived() {
        if (!isPending()) {
            throw new IllegalStateException("Request " + requestId + " was already received");
        }
        status = ReorderStatus.RECEIVED;
        receivedDate = new Date();
    }

    public String getRequestId() { return requestId; }
    public Product getProduct() { return product; }
    public Supplier getSupplier() { return supplier; }
    public int getQuantity() { return quantity; }
    public Date getRequestDate() { return requestDate; }
    public Date getReceivedDate() { return receivedDate; }
    public ReorderStatus getStatus() { return status; }

    public String toString() {
        return requestId + " | " + product.getName() + " (" + product.getProductId() + ")"
                + " | Qty: " + quantity + " | Supplier: " + supplier.getName() + " | " + status;
    }
}
