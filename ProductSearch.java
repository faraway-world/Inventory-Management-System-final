import java.util.ArrayList;
import java.util.List;

public class ProductSearch {
    public static List<Product> searchByName(Inventory inventory, String keyword) {
        List<Product> results = new ArrayList<>();
        String normalizedKeyword = keyword.toLowerCase();
        for (Product product : inventory.getAllProducts()) {
            if (product.getName().toLowerCase().contains(normalizedKeyword)) {
                results.add(product);
            }
        }
        return results;
    }
}
