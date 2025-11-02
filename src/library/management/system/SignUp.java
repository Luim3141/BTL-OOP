import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class SignUp extends JFrame {
    private JTextField txtUser, txtPassword;
    private JButton btnSignUp, btnClose;
    private JLabel lblBg;
    private Connection c = Connect.ConnectToDB();

    public SignUp() {
        setTitle("Sign Up");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);

        JLabel lblTitle = new JLabel("Sign Up Now");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(white);
        lblTitle.setBounds(510, 260, 300, 50);
        add(lblTitle);

        JLabel lblUser = new JLabel("User ID");
        lblUser.setFont(font18);
        lblUser.setForeground(white);
        lblUser.setBounds(470, 330, 100, 30);
        add(lblUser);

        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(font18);
        lblPass.setForeground(white);
        lblPass.setBounds(470, 400, 120, 30);
        add(lblPass);

        txtUser = new JTextField();
        txtUser.setFont(font18);
        txtUser.setBounds(580, 325, 250, 40);
        add(txtUser);

        txtPassword = new JTextField();
        txtPassword.setFont(font18);
        txtPassword.setBounds(580, 395, 250, 40);
        add(txtPassword);

        btnSignUp = new JButton("Sign Up");
        btnSignUp.setFont(font18);
        btnSignUp.setBackground(red);
        btnSignUp.setForeground(white);
        btnSignUp.setBounds(620, 470, 150, 40);
        add(btnSignUp);

        btnClose = new JButton(new ImageIcon(getClass().getResource("/img/close icon.png")));
        btnClose.setBounds(1088, 0, 50, 40);
        add(btnClose);

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/login page.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnClose.addActionListener(e -> dispose());
        btnSignUp.addActionListener(e -> register());
    }

    private void register() {
        try (PreparedStatement pst = c.prepareStatement("INSERT INTO library.login(userid, password, role) VALUES (?, ?, 'client')")) {
            pst.setString(1, txtUser.getText());
            pst.setString(2, txtPassword.getText());
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Account created successfully!");
            dispose();
            new SignIn().setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUp().setVisible(true));
    }
}
