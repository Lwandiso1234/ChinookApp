import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class RecommendationTab extends JPanel {

    private JComboBox<String> customerCombo;
    private JLabel totalSpentLabel, totalPurchasesLabel, lastPurchaseLabel, favGenreLabel;
    private DefaultTableModel recModel;

    public RecommendationTab() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Select Customer:"));
        customerCombo = new JComboBox<>();
        customerCombo.addItem("-- Select a Customer --");
        topPanel.add(customerCombo);
        add(topPanel, BorderLayout.NORTH);

      
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(150);

        JPanel summaryPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Spending Summary"));

        summaryPanel.add(new JLabel("Total Spent:"));
        totalSpentLabel = new JLabel("-");
        summaryPanel.add(totalSpentLabel);
        summaryPanel.add(new JLabel("Total Purchases:"));
        totalPurchasesLabel = new JLabel("-");
        summaryPanel.add(totalPurchasesLabel);
        summaryPanel.add(new JLabel("Last Purchase Date:"));
        lastPurchaseLabel = new JLabel("-");
        summaryPanel.add(lastPurchaseLabel);
        summaryPanel.add(new JLabel("Favourite Genre:"));
        favGenreLabel = new JLabel("-");
        summaryPanel.add(favGenreLabel);

        JPanel recPanel = new JPanel(new BorderLayout());
        recPanel.setBorder(BorderFactory.createTitledBorder("Recommended Tracks"));

        String[] columns = {"Track ID", "Track Name", "Album", "Artist"};
        recModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable recTable = new JTable(recModel);
        recPanel.add(new JScrollPane(recTable), BorderLayout.CENTER);

        splitPane.setTopComponent(summaryPanel);
        splitPane.setBottomComponent(recPanel);
        add(splitPane, BorderLayout.CENTER);

        loadCustomerDropdown();

        customerCombo.addActionListener(e -> updateRecommendations());
    }
        private void loadCustomerDropdown() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            String host = System.getenv("CHINOOK_DB_HOST");
            String port = System.getenv("CHINOOK_DB_PORT");
            String name = System.getenv("CHINOOK_DB_NAME");
            String username = System.getenv("CHINOOK_DB_USERNAME");
            String password = System.getenv("CHINOOK_DB_PASSWORD");

            String url = "jdbc:mariadb://" + host + ":" + port + "/" + name;
            Connection conn = DriverManager.getConnection(url, username, password);

            String sql = "SELECT CustomerId, FirstName, LastName FROM Customer ORDER BY LastName";
            Statement sta = conn.createStatement();
            ResultSet res = sta.executeQuery(sql);

            while (res.next()) {
                String item = res.getInt("CustomerId") + ": " +
                              res.getString("FirstName") + " " +
                              res.getString("LastName");
                customerCombo.addItem(item);
            }

            res.close();
            sta.close();
            conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
        private void updateRecommendations() {
        String selected = (String) customerCombo.getSelectedItem();
        if (selected == null || selected.startsWith("--")) {
            clearDisplay();
            return;
        }

        int customerId = Integer.parseInt(selected.split(":")[0]);
        loadSpendingSummary(customerId);
        loadRecommendations(customerId);
    }
        private void clearDisplay() {
        totalSpentLabel.setText("-");
        totalPurchasesLabel.setText("-");
        lastPurchaseLabel.setText("-");
        favGenreLabel.setText("-");
        recModel.setRowCount(0);
    }
        private void loadSpendingSummary(int customerId) {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            String host = System.getenv("CHINOOK_DB_HOST");
            String port = System.getenv("CHINOOK_DB_PORT");
            String name = System.getenv("CHINOOK_DB_NAME");
            String username = System.getenv("CHINOOK_DB_USERNAME");
            String password = System.getenv("CHINOOK_DB_PASSWORD");

            String url = "jdbc:mariadb://" + host + ":" + port + "/" + name;
            Connection conn = DriverManager.getConnection(url, username, password);

            // Total spent and purchases
            String sql = """
                SELECT COUNT(DISTINCT InvoiceId) AS TotalPurchases,
                       SUM(Total) AS TotalSpent,
                       MAX(InvoiceDate) AS LastPurchase
                FROM Invoice
                WHERE CustomerId = ?
                """;

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, customerId);
            ResultSet res = pstmt.executeQuery();

            if (res.next()) {
                totalPurchasesLabel.setText(String.valueOf(res.getInt("TotalPurchases")));
                totalSpentLabel.setText("$" + String.format("%.2f", res.getDouble("TotalSpent")));

                Date lastDate = res.getDate("LastPurchase");
                lastPurchaseLabel.setText(lastDate != null ? lastDate.toString() : "No purchases");
            }
            res.close();
            pstmt.close();

            // Favourite genre
            String genreSql = """
                SELECT g.Name AS Genre, COUNT(*) AS Count
                FROM Invoice i
                JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                JOIN Track t ON il.TrackId = t.TrackId
                JOIN Genre g ON t.GenreId = g.GenreId
                WHERE i.CustomerId = ?
                GROUP BY g.GenreId, g.Name
                ORDER BY Count DESC
                LIMIT 1
                """;

            pstmt = conn.prepareStatement(genreSql);
            pstmt.setInt(1, customerId);
            res = pstmt.executeQuery();

            if (res.next()) {
                favGenreLabel.setText(res.getString("Genre"));
            } else {
                favGenreLabel.setText("No purchases yet");
            }

            res.close();
            pstmt.close();
            conn.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
        private void loadRecommendations(int customerId) {
        recModel.setRowCount(0);

        // First get the favourite genre
        String favGenre = favGenreLabel.getText();
        if (favGenre.equals("-") || favGenre.equals("No purchases yet")) {
            return;
        }

        try {
            Class.forName("org.mariadb.jdbc.Driver");
            String host = System.getenv("CHINOOK_DB_HOST");
            String port = System.getenv("CHINOOK_DB_PORT");
            String name = System.getenv("CHINOOK_DB_NAME");
            String username = System.getenv("CHINOOK_DB_USERNAME");
            String password = System.getenv("CHINOOK_DB_PASSWORD");

            String url = "jdbc:mariadb://" + host + ":" + port + "/" + name;
            Connection conn = DriverManager.getConnection(url, username, password);

            String sql = """
                SELECT DISTINCT t.TrackId, t.Name AS TrackName,
                       a.Title AS Album, ar.Name AS Artist
                FROM Track t
                JOIN Album a ON t.AlbumId = a.AlbumId
                JOIN Artist ar ON a.ArtistId = ar.ArtistId
                JOIN Genre g ON t.GenreId = g.GenreId
                WHERE g.Name = ?
                  AND t.TrackId NOT IN (
                      SELECT il.TrackId
                      FROM Invoice i
                      JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
                      WHERE i.CustomerId = ?
                  )
                LIMIT 5
                """;

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, favGenre);
            pstmt.setInt(2, customerId);
            ResultSet res = pstmt.executeQuery();

            while (res.next()) {
                recModel.addRow(new Object[]{
                    res.getInt("TrackId"),
                    res.getString("TrackName"),
                    res.getString("Album"),
                    res.getString("Artist")
                });
            }

            res.close();
            pstmt.close();
            conn.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}