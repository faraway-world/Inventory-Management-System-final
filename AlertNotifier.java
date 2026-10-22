public interface AlertNotifier {
    void notifyLowStock(Product product, int threshold, ReorderRequest request);
}
