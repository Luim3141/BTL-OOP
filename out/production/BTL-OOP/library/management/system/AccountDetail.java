import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AccountDetail extends JFrame {
    private JTable table;
    private JButton btnClose, btnReload;
    private JLabel lblBg;
    Connection c = Connect.ConnectToDB();

    public AccountDetail() {
        setTitle("Account Details");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);

        JLabel lblTitle = new JLabel("Account Details");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(red);
        lblTitle.setBounds(60, 40, 300, 40);
        add(lblTitle);

        String[] cols = {"User ID", "Password", "Role"};
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

        btnReload.addActionListener(e -> loadAccounts());
        btnClose.addActionListener(e -> dispose());

        loadAccounts();
    }

    private void loadAccounts() {
        try (PreparedStatement pst = c.prepareStatement("SELECT * FROM library.login");
             ResultSet rs = pst.executeQuery()) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("userid"),
                    rs.getString("password"),
                    rs.getString("role")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading account list: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AccountDetail().setVisible(true));
    }
}
