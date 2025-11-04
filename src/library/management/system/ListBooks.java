import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ListBooks extends JFrame {
    private JTable table;
    private JButton btnReload, btnClose;
    private JLabel lblBg;
    Connection c = Connect.ConnectToDB();

    public ListBooks() {
        setTitle("Book List");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);

        JLabel lblTitle = new JLabel("Book List");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(red);
        lblTitle.setBounds(60, 40, 300, 40);
        add(lblTitle);

        String[] cols = {"ID", "Name", "Author", "Publisher", "Price", "Year", "Status", "Issue Date", "Due Date", "Student ID"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(25);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(60, 120, 1020, 550);
        add(scrollPane);

        btnReload = new JButton("Reload");
        btnReload.setFont(font18);
        btnReload.setBackground(red);
        btnReload.setForeground(white);
        btnReload.setBounds(60, 690, 120, 40);
        add(btnReload);

        btnClose = new JButton("Close");
        btnClose.setFont(font18);
        btnClose.setBackground(red);
        btnClose.setForeground(white);
        btnClose.setBounds(200, 690, 120, 40);
        add(btnClose);

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnReload.addActionListener(e -> loadBooks());
        btnClose.addActionListener(e -> dispose());

        loadBooks();
    }

    private void loadBooks() {
        try (PreparedStatement pst = c.prepareStatement("SELECT * FROM library.book");
             ResultSet rs = pst.executeQuery()) {

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("id"), rs.getString("name"), rs.getString("author"),
                    rs.getString("publisher"), rs.getString("price"), rs.getString("year"),
                    rs.getString("status"), rs.getString("issuedate"), rs.getString("duedate"),
                    rs.getString("studentid")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading book list!");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ListBooks().setVisible(true));
    }
}
