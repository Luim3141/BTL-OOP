import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddAccount extends JFrame {

    private JTextField txtUserID;
    private JPasswordField txtPassword;
    private JComboBox<String> cbRole;
    private JButton btnSave, btnClose;
    private JLabel lblTitle, lblBg;

    Connection c = Connect.ConnectToDB();
    PreparedStatement pst;

    public AddAccount() {
        setTitle("Add Account");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);
        Font font18 = new Font("Segoe UI", Font.BOLD, 18);

        lblTitle = new JLabel("Add Account");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(white);
        lblTitle.setBounds(450, 100, 400, 40);
        add(lblTitle);

        JLabel lblUserID = new JLabel("User ID:");
        lblUserID.setFont(font18);
        lblUserID.setForeground(white);
        lblUserID.setBounds(300, 200, 200, 30);
        add(lblUserID);

        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(font18);
        lblPassword.setForeground(white);
        lblPassword.setBounds(300, 260, 200, 30);
        add(lblPassword);

        JLabel lblRole = new JLabel("Role:");
        lblRole.setFont(font18);
        lblRole.setForeground(white);
        lblRole.setBounds(300, 320, 200, 30);
        add(lblRole);

        txtUserID = new JTextField();
        txtUserID.setFont(font18);
        txtUserID.setBounds(480, 200, 300, 35);
        add(txtUserID);

        txtPassword = new JPasswordField();
        txtPassword.setFont(font18);
        txtPassword.setBounds(480, 260, 300, 35);
        add(txtPassword);

        cbRole = new JComboBox<>(new String[]{"admin", "client"});
        cbRole.setFont(font18);
        cbRole.setBounds(480, 320, 300, 35);
        add(cbRole);

        btnSave = new JButton("Save");
        btnSave.setFont(font18);
        btnSave.setForeground(white);
        btnSave.setBackground(red);
        btnSave.setBounds(450, 420, 120, 40);
        add(btnSave);

        btnClose = new JButton("Close");
        btnClose.setFont(font18);
        btnClose.setForeground(white);
        btnClose.setBackground(red);
        btnClose.setBounds(600, 420, 120, 40);
        add(btnClose);

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnSave.addActionListener(e -> saveAccount());
        btnClose.addActionListener(e -> dispose());
    }

    private void saveAccount() {
        try {
            String id = txtUserID.getText();
            String pass = new String(txtPassword.getPassword());
            String role = cbRole.getSelectedItem().toString();

            if (id.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields!");
                return;
            }

            pst = c.prepareStatement("INSERT INTO library.login(userid, password, role) VALUES (?, ?, ?)");
            pst.setString(1, id);
            pst.setString(2, pass);
            pst.setString(3, role);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Account created successfully as " + role.toUpperCase() + "!");
            clearFields();
        } catch (SQLException ex) {
            Logger.getLogger(AddAccount.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void clearFields() {
        txtUserID.setText("");
        txtPassword.setText("");
        cbRole.setSelectedIndex(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AddAccount().setVisible(true));
    }
}
