import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class PerishableProduct extends Product {
    private String expiryDate;

    public PerishableProduct(String productId, String name, double price, int quantity, String expiryDate) {
        super(productId, name, price, quantity);
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Expiry date is required for perishable product");
        }
        this.expiryDate = expiryDate.trim();
        if (getParsedExpiryDate() == null) {
            throw new IllegalArgumentException("Invalid expiry date format. Please use YYYY-MM-DD.");
        }
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Expiry date is required");
        }
        this.expiryDate = expiryDate.trim();
        if (getParsedExpiryDate() == null) {
            throw new IllegalArgumentException("Invalid expiry date format. Please use YYYY-MM-DD.");
        }
    }

    public LocalDate getParsedExpiryDate() {
        if (expiryDate == null) {
            return null;
        }
        try {
            return LocalDate.parse(expiryDate.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public boolean isExpired() {
        LocalDate parsed = getParsedExpiryDate();
        return parsed != null && parsed.isBefore(LocalDate.now());
    }

    public long getDaysUntilExpiry() {
        LocalDate parsed = getParsedExpiryDate();
        if (parsed == null) {
            return Long.MIN_VALUE;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), parsed);
    }

    public boolean isExpiringSoon(int days) {
        LocalDate parsed = getParsedExpiryDate();
        if (parsed == null || isExpired()) {
            return false;
        }
        long daysRemaining = getDaysUntilExpiry();
        return daysRemaining >= 0 && daysRemaining <= days;
    }

    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            System.out.println("Validation Error: Expiry date is required for perishable product " + getName());
            return false;
        }
        if (getParsedExpiryDate() == null) {
            System.out.println("Validation Error: Expiry date must be in YYYY-MM-DD format for " + getName());
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
        String status = "";
        if (isExpired()) {
            status = " [EXPIRED]";
        } else if (isExpiringSoon(7)) {
            status = " [EXPIRING SOON: " + getDaysUntilExpiry() + " day(s) left]";
        }
        System.out.println("ID: " + getProductId() + " | Name: " + getName()
                + " | Type: Perishable | Price: ₹" + getPrice()
                + " | Stock: " + getQuantity() + " | Expiry: " + expiryDate + status);
    }
}