import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ReportTab extends JPanel {

private DefaultTableModel tableModel;

public ReportTab(){
    setLayout(new BorderLayout());

    JLabel title = new JLabel("Total Revenue by Genre", SwingConstants.CENTER);
    title.setFont(new Font("Arial", Font.BOLD, 16));
    add(title, BorderLayout.NORTH);

    String[] columns = {"Genre", "Total Revenue"};
    tableModel = new DefaultTableModel(columns, 0) {
    @Override
    public boolean isCellEditable(int row, int column){
        return false;
        }
    };
    JTable table = new JTable(tableModel);
    JScrollPane scrollPane = new JScrollPane(table);
    add(scrollPane, BorderLayout.CENTER);
    }

public void refreshReport(){
    tableModel.setRowCount(0); // Clear old data
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
            SELECT g.Name AS Genre,
            ROUND(SUM(il.UnitPrice * il.Quantity), 2) AS TotalRevenue
            FROM Genre g
            JOIN Track t ON g.GenreId = t.GenreId
            JOIN InvoiceLine il ON t.TrackId = il.TrackId
            GROUP BY g.GenreId, g.Name
            ORDER BY TotalRevenue DESC
            """;

        Statement sta = conn.createStatement();
        ResultSet res = sta.executeQuery(sql);

        while(res.next()){
           tableModel.addRow(new Object[]{
            res.getString("Genre"),
            "$" + String.format("%.2f", res.getDouble("TotalRevenue"))
             });
        }

        res.close();
        sta.close();
        conn.close();

    } 
    catch(Exception e){
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
    }
    }
}