import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Main {
    static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        public PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && placeholder != null && !placeholder.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                // Translucent gray placeholder text
                g2.setColor(new Color(130, 130, 130, 160));
                Insets insets = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, insets.left + 4, y);
                g2.dispose();
            }
        }
    }

    Inventory inventory = new Inventory(5);
    JTextArea output = new JTextArea(7, 40);

    DatabaseManager databaseManager = initDatabaseManager();
    TransactionManager transactionManager = new TransactionManager(inventory, databaseManager);

    ReorderManager reorderManager = new ReorderManager(inventory, new AlertNotifier() {
        @Override
        public void notifyLowStock(Product product, int threshold, ReorderRequest request) {
            output.append("\n[LOW STOCK] " + product.getName() + " (" + product.getProductId()
                    + ") qty=" + product.getQuantity() + ". Reorder " + request.getRequestId()
                    + " created for " + request.getQuantity() + " units.");
        }
    }, 20);

    JComboBox<String> pendingOrders = new JComboBox<>();
    JTextField thresholdField = new JTextField();
    JTextField targetField = new JTextField();

    JComboBox<String> saleProduct = new JComboBox<>();
    JComboBox<String> stockProduct = new JComboBox<>();
    JComboBox<String> productSupplier = new JComboBox<>();
    JComboBox<String> productType = new JComboBox<>(new String[] {"Perishable", "Non-Perishable"});

    JTextField supplierId = new JTextField();
    JTextField supplierName = new JTextField();
    JTextField supplierPhone = new JTextField();
    JTextField supplierEmail = new JTextField();

    JTextField productId = new JTextField();
    JTextField productName = new JTextField();
    JTextField productPrice = new JTextField();
    JTextField productQuantity = new JTextField();
    JTextField expiryDate = new PlaceholderTextField("YYYY-MM-DD");
    JTextField warrantyMonths = new PlaceholderTextField("e.g. 12");

    JTextField searchText = new JTextField();
    JTextField stockAmount = new JTextField();
    JTextField saleId = new JTextField();
    JTextField saleQuantity = new JTextField();

    private DatabaseManager initDatabaseManager() {
        try {
            DatabaseManager db = new DatabaseManager();
            System.out.println("Connected to MySQL database.");
            return db;
        } catch (Exception e) {
            System.out.println("MySQL unavailable (" + e.getMessage() + "). Running in in-memory mode.");
            return null;
        }
    }

    private void loadDatabaseData() {
        if (databaseManager != null) {
            inventory.setDatabaseManager(databaseManager);
            reorderManager.setDatabaseManager(databaseManager);
            try {
                // 1. Load suppliers
                List<Supplier> suppliers = databaseManager.loadSuppliers();
                for (Supplier s : suppliers) {
                    inventory.loadSupplierFromDb(s);
                }

                // 2. Load products
                List<DatabaseManager.ProductRecord> products = databaseManager.loadProducts();
                for (DatabaseManager.ProductRecord pr : products) {
                    inventory.loadProductFromDb(pr.getProduct(), pr.getSupplierId());
                }

                // 3. Load past sales
                List<Sale> sales = databaseManager.loadSales(inventory);
                transactionManager.loadTransactionHistoryFromDb(sales);

                // 4. Load reorder requests
                List<ReorderRequest> reorders = databaseManager.loadReorderRequests(inventory);
                reorderManager.loadRequestsFromDb(reorders);

                output.append(String.format("[System] MySQL connected. Restored %d supplier(s), %d product(s), %d sale(s), and %d reorder(s).\n",
                        suppliers.size(), products.size(), sales.size(), reorders.size()));
            } catch (Exception e) {
                output.append("[System] Error restoring database records: " + e.getMessage() + "\n");
            }
        } else {
            output.append("[System] Running in in-memory mode (MySQL unavailable).\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Main().showWindow();
            }
        });
    }

    void showWindow() {
        JFrame frame = new JFrame("Inventory Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(920, 680);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Suppliers", supplierPanel());
        tabs.addTab("Products", productPanel());
        tabs.addTab("Search", searchPanel());
        tabs.addTab("Stock", stockPanel());
        tabs.addTab("Sales", salePanel());
        tabs.addTab("Reorders", reorderPanel());
        tabs.addTab("Reports", reportPanel());

        output.setEditable(false);
        output.setLineWrap(true);

        loadDatabaseData();
        refreshSuppliers();
        refreshProducts();
        refreshPendingOrders();
        updateProductTypeFields();

        frame.add(tabs, BorderLayout.CENTER);
        frame.add(new JScrollPane(output), BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    JPanel supplierPanel() {
        JPanel panel = newForm();
        addField(panel, "Supplier ID", supplierId);
        addField(panel, "Name", supplierName);
        addField(panel, "Phone", supplierPhone);
        addField(panel, "Email", supplierEmail);

        JButton addBtn = new JButton("Add Supplier");
        addBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String id = supplierId.getText().trim();
                    String name = supplierName.getText().trim();
                    String phone = supplierPhone.getText().trim();
                    String email = supplierEmail.getText().trim();

                    Supplier s = new Supplier(id, name, phone, email);
                    inventory.addSupplier(s);
                    refreshSuppliers();
                    supplierId.setText("");
                    supplierName.setText("");
                    supplierPhone.setText("");
                    supplierEmail.setText("");
                    output.setText("Supplier added: " + s.getName() + " (" + s.getId() + ")");
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    error(ex.getMessage());
                }
            }
        });

        JButton deleteBtn = new JButton("Delete Supplier by ID");
        deleteBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = supplierId.getText().trim();
                if (id.isEmpty()) {
                    error("Enter Supplier ID in the field above to delete.");
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to delete supplier " + id + "?",
                        "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        inventory.removeSupplier(id);
                        refreshSuppliers();
                        supplierId.setText("");
                        output.setText("Supplier " + id + " deleted.");
                    } catch (Exception ex) {
                        error(ex.getMessage());
                    }
                }
            }
        });

        panel.add(addBtn);
        panel.add(deleteBtn);
        return panel;
    }

    void updateProductTypeFields() {
        boolean isPerishable = "Perishable".equals(productType.getSelectedItem());
        expiryDate.setEnabled(isPerishable);
        warrantyMonths.setEnabled(!isPerishable);
        if (isPerishable) {
            warrantyMonths.setText("");
        } else {
            expiryDate.setText("");
        }
    }

    JPanel productPanel() {
        JPanel panel = newForm();
        addField(panel, "Product ID", productId);
        addField(panel, "Name", productName);
        addField(panel, "Price", productPrice);
        addField(panel, "Quantity", productQuantity);
        addField(panel, "Type", productType);
        addField(panel, "Supplier", productSupplier);
        addField(panel, "Expiry date (perishable)", expiryDate);
        addField(panel, "Warranty months (non-perishable)", warrantyMonths);

        productType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateProductTypeFields();
            }
        });

        JButton addBtn = new JButton("Add Product");
        addBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String supplier = (String) productSupplier.getSelectedItem();
                if (supplier == null) {
                    error("Add a supplier first.");
                    return;
                }
                try {
                    String id = productId.getText().trim();
                    String name = productName.getText().trim();
                    double price = Double.parseDouble(productPrice.getText().trim());
                    int qty = Integer.parseInt(productQuantity.getText().trim());

                    Product p;
                    if ("Perishable".equals(productType.getSelectedItem())) {
                        String expiry = expiryDate.getText().trim();
                        if (expiry.isEmpty()) {
                            error("Expiry date is required for perishable products. Format: YYYY-MM-DD.");
                            return;
                        }
                        p = new PerishableProduct(id, name, price, qty, expiry);
                    } else {
                        String warrantyText = warrantyMonths.getText().trim();
                        int warranty = warrantyText.isEmpty() ? 0 : Integer.parseInt(warrantyText);
                        p = new NonPerishableProduct(id, name, price, qty, warranty);
                    }

                    inventory.addProduct(p, supplier);
                    checkLowStock(p.getProductId());
                    refreshProducts();
                    productId.setText("");
                    productName.setText("");
                    productPrice.setText("");
                    productQuantity.setText("");
                    expiryDate.setText("");
                    warrantyMonths.setText("");
                    updateProductTypeFields();
                    output.setText("Product added: " + p.getProductId() + " - " + p.getName());
                } catch (NumberFormatException ex) {
                    error("Price, quantity, and warranty must be valid numbers.");
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    error(ex.getMessage());
                }
            }
        });

        JButton deleteBtn = new JButton("Delete Product by ID");
        deleteBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = productId.getText().trim();
                if (id.isEmpty()) {
                    error("Enter Product ID in the field above to delete.");
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to delete product " + id + "?",
                        "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        inventory.removeProduct(id);
                        refreshProducts();
                        productId.setText("");
                        output.setText("Product " + id + " deleted.");
                    } catch (Exception ex) {
                        error(ex.getMessage());
                    }
                }
            }
        });

        panel.add(addBtn);
        panel.add(deleteBtn);
        return panel;
    }

    JPanel searchPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(new JLabel("Product name:"), BorderLayout.WEST);
        top.add(searchText, BorderLayout.CENTER);

        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<Product> found = ProductSearch.searchByName(inventory, searchText.getText().trim());
                if (found.isEmpty()) {
                    output.setText("No products found matching: " + searchText.getText().trim());
                    return;
                }
                StringBuilder text = new StringBuilder();
                text.append(String.format("%-8s | %-18s | %-14s | %6s | %10s | %s\n",
                        "ID", "Name", "Type", "Stock", "Price", "Details"));
                text.append("----------------------------------------------------------------------------------------------------\n");
                for (Product p : found) {
                    String details = "";
                    if (p instanceof PerishableProduct) {
                        PerishableProduct per = (PerishableProduct) p;
                        if (per.isExpired()) {
                            details = "Exp: " + per.getExpiryDate() + " [EXPIRED]";
                        } else if (per.isExpiringSoon(7)) {
                            details = "Exp: " + per.getExpiryDate() + " [" + per.getDaysUntilExpiry() + "d left]";
                        } else {
                            details = "Exp: " + per.getExpiryDate();
                        }
                    } else if (p instanceof NonPerishableProduct) {
                        details = "Warranty: " + ((NonPerishableProduct) p).getWarrantyMonths() + " mo";
                    }
                    text.append(String.format("%-8s | %-18s | %-14s | %6d | ₹%9.2f | %s\n",
                            p.getProductId(), p.getName(), p.getProductType(), p.getQuantity(), p.getPrice(), details));
                }
                output.setText(text.toString());
            }
        });

        top.add(searchBtn, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        return panel;
    }

    JPanel stockPanel() {
        JPanel panel = newForm();
        addField(panel, "Product", stockProduct);
        addField(panel, "Quantity", stockAmount);

        JButton restockBtn = new JButton("Restock");
        restockBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = (String) stockProduct.getSelectedItem();
                if (id == null) {
                    error("Add a product first.");
                    return;
                }
                try {
                    int qty = Integer.parseInt(stockAmount.getText().trim());
                    inventory.restock(id, qty);
                    output.setText(id + " now has " + inventory.getProduct(id).getQuantity() + " in stock.");
                    stockAmount.setText("");
                } catch (NumberFormatException ex) {
                    error("Quantity must be a whole number.");
                } catch (IllegalArgumentException ex) {
                    error(ex.getMessage());
                }
            }
        });

        JButton deductBtn = new JButton("Deduct Stock");
        deductBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = (String) stockProduct.getSelectedItem();
                if (id == null) {
                    error("Add a product first.");
                    return;
                }
                try {
                    int qty = Integer.parseInt(stockAmount.getText().trim());
                    inventory.deductStock(id, qty);
                    output.setText(id + " now has " + inventory.getProduct(id).getQuantity() + " in stock.");
                    checkLowStock(id);
                    stockAmount.setText("");
                } catch (NumberFormatException ex) {
                    error("Quantity must be a whole number.");
                } catch (IllegalArgumentException ex) {
                    error(ex.getMessage());
                }
            }
        });

        panel.add(restockBtn);
        panel.add(deductBtn);
        return panel;
    }

    JPanel salePanel() {
        JPanel panel = newForm();
        addField(panel, "Sale ID", saleId);
        addField(panel, "Product", saleProduct);
        addField(panel, "Quantity", saleQuantity);

        JButton sellBtn = new JButton("Process Sale");
        sellBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = (String) saleProduct.getSelectedItem();
                if (id == null) {
                    error("Add a product first.");
                    return;
                }
                String sid = saleId.getText().trim();
                if (sid.isEmpty()) {
                    error("Sale ID cannot be empty.");
                    return;
                }
                if (transactionManager.hasSaleId(sid)) {
                    error("Sale ID \"" + sid + "\" already exists. Use a unique Sale ID.");
                    return;
                }
                try {
                    int qty = Integer.parseInt(saleQuantity.getText().trim());
                    Sale sale = new Sale(sid);
                    sale.addSaleItem(new SaleItem(inventory.getProduct(id), qty));

                    if (transactionManager.processSale(sale)) {
                        String dbNote = transactionManager.getDatabaseManager() != null ? " (saved to MySQL)" : " (in-memory)";
                        output.setText("Sale processed successfully. Total Bill: ₹" + sale.getTotalAmount() + dbNote);
                        saleId.setText("");
                        saleQuantity.setText("");
                        checkLowStock(id);
                    } else {
                        error("Sale failed. Check the stock.");
                    }
                } catch (NumberFormatException ex) {
                    error("Quantity must be a whole number.");
                } catch (RuntimeException ex) {
                    error(ex.getMessage());
                }
            }
        });

        JButton historyBtn = new JButton("View Sales History");
        historyBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                output.setText(transactionManager.salesReport());
            }
        });

        panel.add(new JLabel());
        panel.add(sellBtn);
        panel.add(new JLabel());
        panel.add(historyBtn);
        return panel;
    }

    JPanel reorderPanel() {
        thresholdField.setText(String.valueOf(reorderManager.getThreshold()));
        targetField.setText(String.valueOf(reorderManager.getTargetStock()));

        JPanel panel = newForm();
        addField(panel, "Low-stock threshold", thresholdField);
        addField(panel, "Reorder up to (target stock)", targetField);

        JButton applyBtn = new JButton("Apply Settings");
        applyBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int threshold = Integer.parseInt(thresholdField.getText().trim());
                    int target = Integer.parseInt(targetField.getText().trim());
                    reorderManager.configure(threshold, target);
                    output.setText("Settings saved. Alert when stock is " + threshold
                            + " or less. Reorders fill up to " + target + ".");
                } catch (NumberFormatException ex) {
                    error("Threshold and target must be whole numbers.");
                } catch (IllegalArgumentException ex) {
                    error(ex.getMessage());
                }
            }
        });

        JButton checkBtn = new JButton("Check All Stock");
        checkBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<ReorderRequest> created = reorderManager.checkAll();
                refreshPendingOrders();
                if (created.isEmpty()) {
                    output.setText("No new reorder requests needed. All stock is above threshold.");
                } else {
                    StringBuilder sb = new StringBuilder(created.size() + " reorder request(s) created:\n");
                    for (ReorderRequest r : created) {
                        sb.append("  ").append(r).append("\n");
                    }
                    output.setText(sb.toString());
                    JOptionPane.showMessageDialog(null,
                            created.size() + " low-stock alert(s). See output log for details.",
                            "Low Stock Alerts", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        JButton receiveBtn = new JButton("Receive Order");
        receiveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String requestId = (String) pendingOrders.getSelectedItem();
                if (requestId == null) {
                    error("There are no pending orders.");
                    return;
                }
                try {
                    reorderManager.receiveOrder(requestId);
                    refreshPendingOrders();
                    output.setText("Order " + requestId + " received. Stock updated successfully.");
                } catch (RuntimeException ex) {
                    error(ex.getMessage());
                }
            }
        });

        JButton showBtn = new JButton("Show Requests");
        showBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                output.setText(reorderManager.requestReport());
            }
        });

        panel.add(new JLabel());
        panel.add(applyBtn);
        panel.add(new JLabel());
        panel.add(checkBtn);
        addField(panel, "Pending order", pendingOrders);
        panel.add(receiveBtn);
        panel.add(showBtn);
        return panel;
    }

    JPanel reportPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JButton stockBtn = new JButton("Stock Report");
        stockBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                output.setText(inventory.stockReport());
            }
        });

        JButton lowBtn = new JButton("Low Stock Report");
        lowBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                output.setText(inventory.lowStockReport());
            }
        });

        JButton expiryBtn = new JButton("Expiry Report");
        expiryBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                output.setText(inventory.expiryReport());
            }
        });

        panel.add(stockBtn);
        panel.add(lowBtn);
        panel.add(expiryBtn);
        return panel;
    }

    JPanel newForm() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    void addField(JPanel panel, String label, Component field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    void refreshSuppliers() {
        productSupplier.removeAllItems();
        for (Supplier s : inventory.getAllSuppliers()) {
            productSupplier.addItem(s.getId());
        }
    }

    void refreshProducts() {
        saleProduct.removeAllItems();
        stockProduct.removeAllItems();
        for (Product p : inventory.getAllProducts()) {
            boolean isExpired = (p instanceof PerishableProduct) && ((PerishableProduct) p).isExpired();
            if (!isExpired) {
                saleProduct.addItem(p.getProductId());
            }
            stockProduct.addItem(p.getProductId());
        }
    }

    void refreshPendingOrders() {
        pendingOrders.removeAllItems();
        for (ReorderRequest request : reorderManager.getPendingRequests()) {
            pendingOrders.addItem(request.getRequestId());
        }
    }

    void checkLowStock(String productId) {
        ReorderRequest request = reorderManager.checkProduct(productId);
        if (request != null) {
            refreshPendingOrders();
            showAlert(inventory.getProduct(productId), reorderManager.getThreshold(), request);
        }
    }

    void showAlert(Product product, int threshold, ReorderRequest request) {
        String message = "Low stock: " + product.getName() + " (" + product.getProductId() + ") has "
                + product.getQuantity() + " left. Threshold is " + threshold + ".\n"
                + "Reorder request " + request.getRequestId() + " created for "
                + request.getQuantity() + " units from " + request.getSupplier().getName() + ".";
        JOptionPane.showMessageDialog(null, message, "Low Stock Alert", JOptionPane.WARNING_MESSAGE);
    }

    void error(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
