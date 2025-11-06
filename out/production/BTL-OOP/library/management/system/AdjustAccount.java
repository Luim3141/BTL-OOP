import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AdjustAccount extends JFrame {
    private JTextField txtUserId, txtPassword;
    private JComboBox<String> comboRole;
    private JButton btnSearch, btnUpdate, btnClose;
    private JLabel lblBg;
    Connection c = Connect.ConnectToDB();
    private boolean canEdit = false;

    public AdjustAccount() {
        setTitle("Adjust Account");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);

        JLabel lblTitle = new JLabel("Adjust Account");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(red);
        lblTitle.setBounds(420, 60, 300, 40);
        add(lblTitle);

        JLabel lblUserId = new JLabel("User ID:");
        JLabel lblPassword = new JLabel("Password:");
        JLabel lblRole = new JLabel("Role:");

        JLabel[] labels = {lblUserId, lblPassword, lblRole};
        int y = 180;
        for (JLabel lbl : labels) {
            lbl.setFont(font18);
            lbl.setForeground(Color.black);
            lbl.setBounds(280, y, 150, 40);
            add(lbl);
            y += 70;
        }

        txtUserId = new JTextField();
        txtPassword = new JTextField();
        comboRole = new JComboBox<>(new String[]{"client", "admin"}); // 🔒 chỉ cho chọn 2 role

        txtUserId.setFont(font18);
        txtUserId.setBounds(420, 180, 350, 40);
        add(txtUserId);

        txtPassword.setFont(font18);
        txtPassword.setBounds(420, 250, 350, 40);
        add(txtPassword);

        comboRole.setFont(font18);
        comboRole.setBounds(420, 320, 350, 40);
        add(comboRole);

        btnSearch = new JButton("Search");
        btnSearch.setFont(font18);
        btnSearch.setBackground(red);
        btnSearch.setForeground(white);
        btnSearch.setBounds(780, 180, 120, 40);
        add(btnSearch);

        btnUpdate = new JButton("Update");
        btnUpdate.setFont(font18);
        btnUpdate.setBackground(red);
        btnUpdate.setForeground(white);
        btnUpdate.setBounds(420, 480, 130, 40);
        add(btnUpdate);

        btnClose = new JButton("Close");
        btnClose.setFont(font18);
        btnClose.setBackground(red);
        btnClose.setForeground(white);
        btnClose.setBounds(570, 480, 130, 40);
        add(btnClose);

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnSearch.addActionListener(e -> searchAccount());
        btnUpdate.addActionListener(e -> updateAccount());
        btnClose.addActionListener(e -> dispose());
    }

    private void searchAccount() {
        canEdit = false;
        txtPassword.setText("");
        comboRole.setSelectedIndex(0);

        try (PreparedStatement pst = c.prepareStatement("SELECT * FROM library.login WHERE userid=?")) {
            pst.setString(1, txtUserId.getText());
            ResultSet rs = pst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "❌ No account found with User ID: " + txtUserId.getText());
                return;
            }

            String role = rs.getString("role");
            if (role.equalsIgnoreCase("admin")) {
                JOptionPane.showMessageDialog(this, "⚠️ Cannot modify Admin account!");
                return;
            }

            txtPassword.setText(rs.getString("password"));
            comboRole.setSelectedItem(role); // 🟢 tự động chọn role trong combobox
            canEdit = true;

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void updateAccount() {
        if (!canEdit) {
            JOptionPane.showMessageDialog(this, "❌ Cannot update this account!");
            return;
        }

        try (PreparedStatement pst = c.prepareStatement(
                "UPDATE library.login SET password=?, role=? WHERE userid=?")) {
            pst.setString(1, txtPassword.getText());
            pst.setString(2, comboRole.getSelectedItem().toString()); // 🟢 lấy role từ combobox
            pst.setString(3, txtUserId.getText());

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ Account updated successfully!");
            txtPassword.setText("");
            comboRole.setSelectedIndex(0);
            canEdit = false;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdjustAccount().setVisible(true));
    }
}
