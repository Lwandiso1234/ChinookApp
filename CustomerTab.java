import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CustomerTab extends JPanel{

    private DefaultTableModel tableModel;
    private JTable customerTable;
    private DefaultTableModel inactiveModel;
    private JTable inactiveTable;
    private JTextField searchField;

    public CustomerTab(){
        setLayout(new BorderLayout());

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        JButton searchBtn = new JButton("Search");
        searchPanel.add(searchBtn);
        add(searchPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(350);
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createBtn = new JButton("Create");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        buttonPanel.add(createBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        topPanel.add(buttonPanel, BorderLayout.NORTH);

        String[] columns = {"ID", "First Name", "Last Name", "Email", "Phone", "Country"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        customerTable = new JTable(tableModel);
        topPanel.add(new JScrollPane(customerTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel inactiveLabel = new JLabel(" Inactive Customers (no purchase in 2+ years)");
        inactiveLabel.setFont(new Font("Arial", Font.BOLD, 13));
        bottomPanel.add(inactiveLabel, BorderLayout.NORTH);

        String[] inactiveCols = {"ID", "First Name", "Last Name", "Email", "Phone", "Country"};
        inactiveModel = new DefaultTableModel(inactiveCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        inactiveTable = new JTable(inactiveModel);
        bottomPanel.add(new JScrollPane(inactiveTable), BorderLayout.CENTER);
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(bottomPanel);
        add(splitPane, BorderLayout.CENTER);

        loadCustomers("");
        loadInactiveCustomers();
        createBtn.addActionListener(e -> openCustomerDialog(null));
        updateBtn.addActionListener(e -> updateSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        searchBtn.addActionListener(e -> loadCustomers(searchField.getText()));
        searchField.addActionListener(e -> loadCustomers(searchField.getText()));
    }

    private void loadCustomers(String filter){
        tableModel.setRowCount(0);
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
                SELECT CustomerId, FirstName, LastName, Email, Phone, Country
                FROM Customer
                ORDER BY LastName""";

            Statement sta = conn.createStatement();
            ResultSet res = sta.executeQuery(sql);

            while(res.next()){
                String fn = res.getString("FirstName");
                String ln = res.getString("LastName");
                String full = (fn + " " + ln).toLowerCase();

                if(filter.isEmpty() || full.contains(filter.toLowerCase())){
                    tableModel.addRow(new Object[]{
                        res.getInt("CustomerId"), fn, ln,
                        res.getString("Email"),
                        res.getString("Phone"),
                        res.getString("Country")
                    });
                }
            }
            res.close();
            sta.close();
            conn.close();
        }
        catch (Exception e){
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void loadInactiveCustomers() {
        inactiveModel.setRowCount(0);
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
                SELECT c.CustomerId, c.FirstName, c.LastName, c.Email, c.Phone, c.Country
                FROM Customer c
                WHERE c.CustomerId NOT IN (SELECT DISTINCT CustomerId FROM Invoice)
                   OR c.CustomerId IN (
                       SELECT CustomerId FROM Invoice
                       GROUP BY CustomerId
                       HAVING MAX(InvoiceDate) < DATE_SUB(CURDATE(), INTERVAL 2 YEAR)
                   )
                ORDER BY c.LastName
                """;

            Statement sta = conn.createStatement();
            ResultSet res = sta.executeQuery(sql);

            while (res.next()) {
                inactiveModel.addRow(new Object[]{
                    res.getInt("CustomerId"),
                    res.getString("FirstName"),
                    res.getString("LastName"),
                    res.getString("Email"),
                    res.getString("Phone"),
                    res.getString("Country")
                });
            }

            res.close();
            sta.close();
            conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void openCustomerDialog(Customer existing) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),existing == null ? "Create Customer" : "Update Customer", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        dialog.setLocationRelativeTo(this);

        JTextField fnField = new JTextField(existing != null ? existing.getFirstName() : "");
        JTextField lnField = new JTextField(existing != null ? existing.getLastName() : "");
        JTextField emField = new JTextField(existing != null ? existing.getEmail() : "");
        JTextField phField = new JTextField(existing != null ? existing.getPhone() : "");
        JTextField coField = new JTextField(existing != null ? existing.getCountry() : "");

        dialog.add(new JLabel("First Name:"));
        dialog.add(fnField);
        dialog.add(new JLabel("Last Name:"));
        dialog.add(lnField);
        dialog.add(new JLabel("Email:"));
        dialog.add(emField);
        dialog.add(new JLabel("Phone:"));
        dialog.add(phField);
        dialog.add(new JLabel("Country:"));
        dialog.add(coField);

        JButton saveBtn = new JButton("Save");
        dialog.add(saveBtn);

        saveBtn.addActionListener(e -> {
            try{
                Class.forName("org.mariadb.jdbc.Driver");
                String host = System.getenv("CHINOOK_DB_HOST");
                String port = System.getenv("CHINOOK_DB_PORT");
                String name = System.getenv("CHINOOK_DB_NAME");
                String username = System.getenv("CHINOOK_DB_USERNAME");
                String password = System.getenv("CHINOOK_DB_PASSWORD");

                String url = "jdbc:mariadb://" + host + ":" + port + "/" + name;
                Connection conn = DriverManager.getConnection(url, username, password);

                if(existing == null){
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT MAX(CustomerId) + 1 AS NextId FROM Customer");
                    int nextId = 1;
                    if (rs.next()) nextId = rs.getInt("NextId");
                    rs.close();
                    stmt.close();

                    String sql = "INSERT INTO Customer (CustomerId, FirstName, LastName, Email, Phone, Country) VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setInt(1, nextId);
                    pstmt.setString(2, fnField.getText());
                    pstmt.setString(3, lnField.getText());
                    pstmt.setString(4, emField.getText());
                    pstmt.setString(5, phField.getText());
                    pstmt.setString(6, coField.getText());
                    pstmt.executeUpdate();
                    pstmt.close();
                } 
                else{
                    String sql = "UPDATE Customer SET FirstName=?, LastName=?, Email=?, Phone=?, Country=? WHERE CustomerId=?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, fnField.getText());
                    pstmt.setString(2, lnField.getText());
                    pstmt.setString(3, emField.getText());
                    pstmt.setString(4, phField.getText());
                    pstmt.setString(5, coField.getText());
                    pstmt.setInt(6, existing.getId());
                    pstmt.executeUpdate();
                    pstmt.close();
                }

                conn.close();
                dialog.dispose();
                loadCustomers(searchField.getText());
                loadInactiveCustomers();

            }
            catch(Exception ex){
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        dialog.setVisible(true);
    }

    private void updateSelected(){
        int row = customerTable.getSelectedRow();
        if(row == -1){
            JOptionPane.showMessageDialog(this, "Select a customer first.");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String fn = (String) tableModel.getValueAt(row, 1);
        String ln = (String) tableModel.getValueAt(row, 2);
        String em = (String) tableModel.getValueAt(row, 3);
        String ph = (String) tableModel.getValueAt(row, 4);
        String co = (String) tableModel.getValueAt(row, 5);

        Customer existing = new Customer(id, fn, ln, em, ph, co);
        openCustomerDialog(existing);
    }

    private void deleteSelected(){
        int row = customerTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a customer first.");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String name = tableModel.getValueAt(row, 1) + " " + tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + name + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Class.forName("org.mariadb.jdbc.Driver");
                String host = System.getenv("CHINOOK_DB_HOST");
                String port = System.getenv("CHINOOK_DB_PORT");
                String nameDB = System.getenv("CHINOOK_DB_NAME");
                String username = System.getenv("CHINOOK_DB_USERNAME");
                String password = System.getenv("CHINOOK_DB_PASSWORD");

                String url = "jdbc:mariadb://" + host + ":" + port + "/" + nameDB;
                Connection conn = DriverManager.getConnection(url, username, password);

                String sql = "DELETE FROM Customer WHERE CustomerId = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
                pstmt.close();
                conn.close();

                loadCustomers(searchField.getText());
                loadInactiveCustomers();
            }
            catch(Exception e){
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
}