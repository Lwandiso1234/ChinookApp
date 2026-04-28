import java.awt.*;
import java.sql.*;
import javax.swing.*;

class TracksTab extends JPanel{
private JTextArea trackArea;

public TracksTab(){
    setLayout(new BorderLayout());
    JButton addButton = new JButton("+ new Track");
    add(addButton,BorderLayout.NORTH);

    trackArea = new JTextArea();
    trackArea.setEditable(false);
    JScrollPane scrollPane = new JScrollPane(trackArea);
    add(scrollPane, BorderLayout.CENTER);
    loadTracks();

    addButton.addActionListener(e->openAddTrackDialog());
    }
private void loadTracks(){
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
        SELECT t.TrackId, t.Name AS TrackName, a.Title AS Album,g.Name AS Genre, m.Name AS MediaType, t.UnitPrice
        FROM Track t
        JOIN Album a ON t.AlbumId = a.AlbumId
        JOIN Genre g ON t.GenreId = g.GenreId
        JOIN MediaType m ON t.MediaTypeId = m.MediaTypeId
        ORDER BY t.TrackId DESC
        LIMIT 50
        """;

        Statement sta = conn.createStatement();
        ResultSet res = sta.executeQuery(sql);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %-55s %-60s %-25s %-30s %-8s\n","ID", "Track Name", "Album", "Genre", "Media Type", "Price"));
        sb.append("-".repeat(130)).append("\n");

        while(res.next()){
            sb.append(String.format("%-8d %-55s %-60s %-25s %-30s $%-7.2f\n",res.getInt("TrackId"),res.getString("TrackName"),
            res.getString("Album"),res.getString("Genre"),res.getString("MediaType"),res.getDouble("UnitPrice")));
        }
        trackArea.setText(sb.toString());
        res.close();
        sta.close();
        conn.close();

        }
        catch(Exception e){
            trackArea.setText("Error: " + e.getMessage());
        }
    }
private void openAddTrackDialog() {
        // Create a popup dialog
    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),"Add New Track", true);
    dialog.setSize(450, 400);
    dialog.setLayout(new GridLayout(9, 2, 10, 10));
    dialog.setLocationRelativeTo(this);
    JTextField nameField = new JTextField();
    JTextField composerField = new JTextField();
    JTextField millisField = new JTextField();
    JTextField bytesField = new JTextField();
    JTextField priceField = new JTextField("0.99");

    JComboBox<String> albumCombo = new JComboBox<>();
    JComboBox<String> genreCombo = new JComboBox<>();
    JComboBox<String> mediaCombo = new JComboBox<>();
    populateDropdowns(albumCombo, genreCombo, mediaCombo);

    dialog.add(new JLabel("Track Name:"));dialog.add(nameField);dialog.add(new JLabel("Album:"));
    dialog.add(albumCombo);dialog.add(new JLabel("Genre:"));dialog.add(genreCombo);
    dialog.add(new JLabel("Media Type:"));dialog.add(mediaCombo);dialog.add(new JLabel("Composer:"));
    dialog.add(composerField);dialog.add(new JLabel("Milliseconds:"));dialog.add(millisField);
    dialog.add(new JLabel("Bytes:"));dialog.add(bytesField);dialog.add(new JLabel("Unit Price:"));
    dialog.add(priceField);

    JButton saveBtn = new JButton("Save");
    dialog.add(saveBtn);
    dialog.add(new JLabel()); // empty spacer for layout
    saveBtn.addActionListener(e -> {
        saveTrack(dialog, nameField, composerField, millisField,bytesField, priceField, albumCombo, genreCombo, mediaCombo);
        });

    dialog.setVisible(true);
    }
private void populateDropdowns(JComboBox<String> albumCombo,JComboBox<String> genreCombo,JComboBox<String> mediaCombo){
    try{
        Class.forName("org.mariadb.jdbc.Driver");
        String host = System.getenv("CHINOOK_DB_HOST");
        String port = System.getenv("CHINOOK_DB_PORT");
        String name = System.getenv("CHINOOK_DB_NAME");
        String username = System.getenv("CHINOOK_DB_USERNAME");
        String password = System.getenv("CHINOOK_DB_PASSWORD");

        String url = "jdbc:mariadb://" + host + ":" + port + "/" + name;
        Connection conn = DriverManager.getConnection(url, username, password);
        Statement stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT AlbumId, Title FROM Album ORDER BY Title");
        while(rs.next()){
            albumCombo.addItem(rs.getInt("AlbumId") + ": " + rs.getString("Title"));
        }
        rs.close();

        rs = stmt.executeQuery("SELECT GenreId, Name FROM Genre ORDER BY Name");
        while(rs.next()){
            genreCombo.addItem(rs.getInt("GenreId") + ": " + rs.getString("Name"));
        }
        rs.close();

        rs=stmt.executeQuery("SELECT MediaTypeId, Name FROM MediaType ORDER BY Name");
        while(rs.next()){
            mediaCombo.addItem(rs.getInt("MediaTypeId") + ": " + rs.getString("Name"));
            }
        rs.close();

        stmt.close();
        conn.close();
        } 
        catch(Exception e){
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
        }
    }
private void saveTrack(JDialog dialog, JTextField nameField, JTextField composerField,JTextField millisField, JTextField bytesField, JTextField priceField,
    JComboBox<String> albumCombo, JComboBox<String> genreCombo,JComboBox<String> mediaCombo){
    try {
        Class.forName("org.mariadb.jdbc.Driver");

        String host = System.getenv("CHINOOK_DB_HOST");
        String port = System.getenv("CHINOOK_DB_PORT");
        String name = System.getenv("CHINOOK_DB_NAME");
        String username = System.getenv("CHINOOK_DB_USERNAME");
        String password = System.getenv("CHINOOK_DB_PASSWORD");

        String url = "jdbc:mariadb://" + host + ":" + port + "/" + name;
        Connection conn = DriverManager.getConnection(url, username, password);

        int nextId = 1;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT MAX(TrackId) + 1 AS NextId FROM Track");
        if(rs.next()){
            nextId = rs.getInt("NextId");
        }
        rs.close();
        stmt.close();

        int albumId = Integer.parseInt(((String) albumCombo.getSelectedItem()).split(":")[0].trim());
        int genreId = Integer.parseInt(((String) genreCombo.getSelectedItem()).split(":")[0].trim());
        int mediaId = Integer.parseInt(((String) mediaCombo.getSelectedItem()).split(":")[0].trim());

        String sql = """
        INSERT INTO Track (TrackId, Name, AlbumId, MediaTypeId, GenreId,Composer, Milliseconds, Bytes, UnitPrice)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, nextId);pstmt.setString(2, nameField.getText());pstmt.setInt(3, albumId);
        pstmt.setInt(4, mediaId);pstmt.setInt(5, genreId);pstmt.setString(6, composerField.getText());
        pstmt.setInt(7, Integer.parseInt(millisField.getText()));
        pstmt.setInt(8, Integer.parseInt(bytesField.getText()));pstmt.setDouble(9, Double.parseDouble(priceField.getText()));
        pstmt.executeUpdate();

        pstmt.close();
        conn.close();

            // Show success message
        JOptionPane.showMessageDialog(dialog, "Track saved successfully with ID: " + nextId);

            // Close the dialog
        dialog.dispose();

            // Refresh the track list
        loadTracks();
    } 
    catch(Exception e){
        JOptionPane.showMessageDialog(dialog, "Error saving track: " + e.getMessage());
        }
    }
}