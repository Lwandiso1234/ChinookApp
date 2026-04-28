/* 
COS221 Prac4 By Lwandiso_George(u25405064)
Title: Chinook music app
*/


import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class ChinookApp extends JFrame{

    private JTextArea textArea;

    private void loadEmployees(String filter){
        try{
            Class.forName("org.mariadb.jdbc.Driver");
            String host = System.getenv("CHINOOK_DB_HOST");
            String port = System.getenv("CHINOOK_DB_PORT");
            String name = System.getenv("CHINOOK_DB_NAME");
            String username = System.getenv("CHINOOK_DB_USERNAME");
            String password = System.getenv("CHINOOK_DB_PASSWORD");

            String url = "jdbc:mariadb://" + host + ":" + port + "/" + name;
            Connection conn = DriverManager.getConnection(url, username, password);

            String sql = """
                SELECT e.FirstName, e.LastName, e.Title, e.City, e.Country, e.Phone,
                IFNULL(CONCAT(s.FirstName,' ',s.LastName),'None') AS Supervisor
                FROM Employee e
                LEFT JOIN Employee s ON e.ReportsTo = s.EmployeeId
                ORDER BY e.LastName
                """;

            Statement sta = conn.createStatement();
            ResultSet res = sta.executeQuery(sql);

            StringBuilder x = new StringBuilder();
            x.append(String.format("%-15s %-15s %-25s %-15s %-15s %-20s %-20s\n","First Name", "Last Name", "Title", "City", "Country", "Phone", "Supervisor"));
            x.append("-".repeat(150)).append("\n");

            while(res.next()){
                String firstName=res.getString("FirstName");
                String lastName=res.getString("LastName");
                String city=res.getString("City");
                String title=res.getString("Title");
                String country=res.getString("Country");
                String phone=res.getString("Phone");
                String supervisor = res.getString("Supervisor");

                String fullname = (firstName + " " + lastName + " " + city).toLowerCase();
                // Only show rows matching filter
                if(filter.isEmpty() || fullname.contains(filter.toLowerCase())){
                    x.append(String.format("%-15s %-15s %-25s %-15s %-15s %-20s %-20s\n",
                    firstName, lastName, title, city, country, phone, supervisor));
                }
            }
            textArea.setText(x.toString());
            res.close();
            sta.close();
            conn.close();
        }
        catch(Exception e){
            textArea.setText("Error: " + e.getMessage());
        }
    }

    public ChinookApp(){
        setTitle("Chinook 🎼𝄞🎶-Store -u25405064@UP ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        // Main panel
        JPanel panel = new JPanel(new BorderLayout());

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by Name or City:"));
        JTextField filterField = new JTextField(20);
        filterPanel.add(filterField);
        JButton filterButton = new JButton("Filter");
        filterPanel.add(filterButton);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Employees", panel);
        tabs.addTab("Tracks", new TracksTab());
        ReportTab reportTab = new ReportTab();
        tabs.addTab("Revenue Report", reportTab);
        tabs.addTab("Customers", new CustomerTab());
        tabs.addTab("Recommendations", new RecommendationTab());

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == reportTab) {
                reportTab.refreshReport();
            }
        });

        add(tabs);

        // Load_a abasebenzi kuqala
        loadEmployees("");
        filterButton.addActionListener(e -> loadEmployees(filterField.getText()));
        // triggering the filter ngoEnter mhlobam
        filterField.addActionListener(e -> loadEmployees(filterField.getText()));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChinookApp().setVisible(true);
        });
    }
}