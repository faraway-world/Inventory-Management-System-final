import java.util.ArrayList;
import java.util.List;

public class ReorderManager {
    private Inventory inventory;
    private AlertNotifier notifier;
    private ArrayList<ReorderRequest> requests = new ArrayList<>();
    private int targetStock;
    private int nextNumber = 1;

    public ReorderManager(Inventory inventory, AlertNotifier notifier, int targetStock) {
        if (targetStock <= inventory.getLowStockLimit()) {
            throw new IllegalArgumentException("Target stock must be more than the low stock limit");
        }
        this.inventory = inventory;
        this.notifier = notifier;
        this.targetStock = targetStock;
    }

    public int getThreshold() {
        return inventory.getLowStockLimit();
    }

    public int getTargetStock() {
        return targetStock;
    }

    public void configure(int threshold, int target) {
        if (target <= threshold) {
            throw new IllegalArgumentException("Target stock must be more than the low stock limit");
        }
        inventory.setLowStockLimit(threshold);
        targetStock = target;
    }

    private ReorderRequest findRequest(String requestId) {
        for (ReorderRequest r : requests) {
            if (r.getRequestId().equals(requestId)) {
                return r;
            }
        }
        return null;
    }

    private boolean hasPendingRequest(String productId) {
        for (ReorderRequest r : requests) {
            if (r.isPending() && r.getProduct().getProductId().equals(productId)) {
                return true;
            }
        }
        return false;
    }

    // Makes a request if stock is low and nothing is already pending for this product.
    // Returns null when no request was needed.
    public ReorderRequest checkProduct(String productId) {
        if (!inventory.isLowStock(productId) || hasPendingRequest(productId)) {
            return null;
        }

        Product product = inventory.getProduct(productId);
        Supplier supplier = inventory.getSupplierOf(productId);

        int quantity = targetStock - product.getQuantity();
        if (quantity < 1) {
            quantity = 1;
        }

        ReorderRequest request = new ReorderRequest("RO" + nextNumber, product, supplier, quantity);
        nextNumber++;
        requests.add(request);
        notifier.notifyLowStock(product, inventory.getLowStockLimit(), request);
        return request;
    }

    public List<ReorderRequest> checkAll() {
        List<ReorderRequest> created = new ArrayList<>();
        for (Product p : inventory.getAllProducts()) {
            ReorderRequest request = checkProduct(p.getProductId());
            if (request != null) {
                created.add(request);
            }
        }
        return created;
    }

    public void receiveOrder(String requestId) {
        ReorderRequest request = findRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("No reorder request with ID " + requestId);
        }
        if (!request.isPending()) {
            throw new IllegalStateException("Request " + requestId + " was already received");
        }
        inventory.restock(request.getProduct().getProductId(), request.getQuantity());
        request.markReceived();
    }

    public List<ReorderRequest> getAllRequests() {
        return new ArrayList<>(requests);
    }

    public List<ReorderRequest> getPendingRequests() {
        List<ReorderRequest> pending = new ArrayList<>();
        for (ReorderRequest r : requests) {
            if (r.isPending()) {
                pending.add(r);
            }
        }
        return pending;
    }

    public String requestReport() {
        String report = "Reorder requests\n";
        if (requests.isEmpty()) {
            report += "No reorder requests yet.\n";
        }
        for (ReorderRequest r : requests) {
            report += r + "\n";
        }
        return report;
    }
}
