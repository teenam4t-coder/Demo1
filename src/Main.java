import java.sql.*;
import java.util.Scanner;

public class Main {

    private static final String URL = "jdbc:mysql://localhost:3306/retail_db";
    private static final String USER = "root";
    private static final String PASSWORD = "admin"; // <-- Yahan apna MySQL password daalo

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Connected to retail_db successfully!");

            while (true) {
                System.out.println("\n--- INVENTORY MANAGEMENT MENU ---");
                System.out.println("1. Insert Product Records (At least 3)");
                System.out.println("2. Search Low-Stock Products by Category (< 20)");
                System.out.println("3. Update Product Stock");
                System.out.println("4. Search and Modify Product Price");
                System.out.println("5. Total Inventory Value & Conditional Delete");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");
                int choice = sc.nextInt();
                sc.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        insertProducts(conn, sc);
                        break;
                    case 2:
                        retrieveAndSort(conn, sc);
                        break;
                    case 3:
                        updateStock(conn, sc);
                        break;
                    case 4:
                        searchAndModifyPrice(conn, sc);
                        break;
                    case 5:
                        aggregateAndDelete(conn, sc);
                        break;
                    case 6:
                        System.out.println("Exiting application...");
                        return;
                    default:
                        System.out.println("Invalid choice! Please try again.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // (a) Insert Product Records
    private static void insertProducts(Connection conn, Scanner sc) {
        String sql = "INSERT INTO Product (product_id, product_name, category, quantity, price) VALUES (?, ?, ?, ?, ?)";
        System.out.print("How many products do you want to insert (minimum 3)? ");
        int count = sc.nextInt();
        sc.nextLine();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                System.out.println("\nEnter details for Product " + i + ":");
                System.out.print("Product ID: ");
                int id = sc.nextInt();
                sc.nextLine();
                System.out.print("Product Name: ");
                String name = sc.nextLine();
                System.out.print("Category: ");
                String category = sc.nextLine();
                System.out.print("Quantity: ");
                int qty = sc.nextInt();
                System.out.print("Price: ");
                double price = sc.nextDouble();
                sc.nextLine();

                pstmt.setInt(1, id);
                pstmt.setString(2, name);
                pstmt.setString(3, category);
                pstmt.setInt(4, qty);
                pstmt.setDouble(5, price);

                try {
                    pstmt.executeUpdate();
                    System.out.println("Product inserted successfully.");
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("Error: Duplicate Product ID (" + id + "). Skipping record.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
    }

    // (b) Conditional Retrieval and Sorting
    private static void retrieveAndSort(Connection conn, Scanner sc) {
        System.out.print("Enter category to search: ");
        String category = sc.nextLine();

        String sql = "SELECT * FROM Product WHERE category = ? AND quantity < 20 ORDER BY price DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- Products in '" + category + "' with Quantity < 20 ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("ID: %d | Name: %s | Qty: %d | Price: %.2f%n",
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"));
            }
            if (!found) {
                System.out.println("No matching products found.");
            }
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
    }

    // (c) Update Product Stock
    private static void updateStock(Connection conn, Scanner sc) {
        System.out.print("Enter Product ID: ");
        int id = sc.nextInt();
        System.out.print("Enter quantity to add: ");
        int addQty = sc.nextInt();

        String sql = "UPDATE Product SET quantity = quantity + ? WHERE product_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, addQty);
            pstmt.setInt(2, id);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Stock updated successfully.");
            } else {
                System.out.println("Error: Product ID " + id + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
    }

    // (d) Search and Modify Product Details
    private static void searchAndModifyPrice(Connection conn, Scanner sc) {
        System.out.print("Enter Product ID: ");
        int id = sc.nextInt();
        System.out.print("Enter new price: ");
        double newPrice = sc.nextDouble();

        if (newPrice <= 0) {
            System.out.println("Error: New price must be greater than zero.");
            return;
        }

        String updateSql = "UPDATE Product SET price = ? WHERE product_id = ?";
        String selectSql = "SELECT * FROM Product WHERE product_id = ?";

        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            updateStmt.setDouble(1, newPrice);
            updateStmt.setInt(2, id);

            int rowsAffected = updateStmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Price updated successfully. Updated Details:");
                selectStmt.setInt(1, id);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    System.out.printf("ID: %d | Name: %s | Category: %s | Qty: %d | Price: %.2f%n",
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getString("category"),
                            rs.getInt("quantity"),
                            rs.getDouble("price"));
                }
            } else {
                System.out.println("Error: Product ID " + id + " not found.");
            }
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
    }

    // (e) Aggregate Query and Conditional Deletion
    private static void aggregateAndDelete(Connection conn, Scanner sc) {
        String sumSql = "SELECT SUM(quantity * price) AS total_value FROM Product";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sumSql)) {
            if (rs.next()) {
                System.out.printf("Total Inventory Value: %.2f%n", rs.getDouble("total_value"));
            }
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }

        System.out.print("Enter Product ID to delete (will only delete if quantity is 0): ");
        int id = sc.nextInt();

        String deleteSql = "DELETE FROM Product WHERE product_id = ? AND quantity = 0";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Product ID " + id + " deleted successfully.");
            } else {
                System.out.println("Cannot delete: Product either does not exist or its quantity is not zero.");
            }
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        }
    }
}