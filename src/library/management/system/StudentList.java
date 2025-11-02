import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StudentList extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private Connection c = Connect.ConnectToDB();

    public StudentList() {
        setTitle("Student List");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ===== TABLE SETUP =====
        String[] columnNames = {
            "ID", "Name", "Course", "Branch", "Semester",
            "Gender", "Class", "Email", "Phone", "Address"
        };
        model = new DefaultTableModel(columnNames, 0);
        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // ===== BUTTON =====
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadStudents());
        add(btnRefresh, BorderLayout.SOUTH);

        // ===== INITIAL LOAD =====
        loadStudents();
    }

    private void loadStudents() {
        model.setRowCount(0); // Clear old data
        try {
            PreparedStatement pst = c.prepareStatement("SELECT * FROM library.student");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("course"),
                    rs.getString("branch"),
                    rs.getString("semester"),
                    rs.getString("gender"),
                    rs.getString("class"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("address")
                });
            }
        } catch (SQLException ex) {
            Logger.getLogger(StudentList.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentList().setVisible(true));
    }
}
