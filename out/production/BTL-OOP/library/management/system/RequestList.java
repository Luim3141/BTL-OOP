import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestList extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private Connection c = Connect.ConnectToDB();

    public RequestList() {
        setTitle("Request List");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ===== TITLE =====
        JLabel lblTitle = new JLabel("List of Client Book Requests", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(60, 63, 65));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(lblTitle, BorderLayout.NORTH);

        // ===== TABLE MODEL =====
        String[] columnNames = {"Request ID", "Book ID", "Book Name", "Student ID", "Student Name"};
        model = new DefaultTableModel(columnNames, 0);
        table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // ===== BUTTON PANEL =====
        JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setBackground(new Color(0, 102, 204));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setPreferredSize(new Dimension(120, 35));
        btnRefresh.addActionListener(e -> loadRequests());
        panelButtons.add(btnRefresh);

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnClose.setBackground(new Color(204, 0, 0));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setPreferredSize(new Dimension(120, 35));
        btnClose.addActionListener(e -> dispose());
        panelButtons.add(btnClose);

        add(panelButtons, BorderLayout.SOUTH);

        // ===== INITIAL LOAD =====
        loadRequests();
    }

    private void loadRequests() {
        model.setRowCount(0);
        try {
            PreparedStatement pst = c.prepareStatement("SELECT * FROM library.request");
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("id"),           // id auto increment
                    rs.getString("book_id"),
                    rs.getString("book_name"),
                    rs.getString("student_id"),
                    rs.getString("student_name")
                });
            }
        } catch (SQLException ex) {
            Logger.getLogger(RequestList.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database error while loading request list.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RequestList().setVisible(true));
    }
}
