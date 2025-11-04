import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.logging.*;
import javax.swing.*;

public class SignIn extends JFrame {
    private JTextField txtemail;
    private JPasswordField txtpassword;
    private JButton btnlogin, btnsignup, btnClose;
    private JLabel lblBg;
    private Connection c = Connect.ConnectToDB();

    public SignIn() {
        setTitle("Sign In");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);


        JLabel lblTitle = new JLabel("Login Now");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(white);
        lblTitle.setBounds(510, 260, 300, 50);
        add(lblTitle);

        JLabel lblUser = new JLabel("User ID");
        lblUser.setFont(font18);
        lblUser.setForeground(Color.blue);
        lblUser.setBounds(470, 330, 100, 30);
        add(lblUser);

        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(font18);
        lblPass.setForeground(Color.blue);
        lblPass.setBounds(470, 400, 120, 30);
        add(lblPass);

        txtemail = new JTextField();
        txtemail.setFont(font18);
        txtemail.setBounds(580, 325, 250, 40);
        add(txtemail);

        txtpassword = new JPasswordField();
        txtpassword.setFont(font18);
        txtpassword.setBounds(580, 395, 250, 40);
        add(txtpassword);

        btnlogin = new JButton("Login");
        btnlogin.setFont(font18);
        btnlogin.setBackground(red);
        btnlogin.setForeground(white);
        btnlogin.setBounds(580, 470, 120, 40);
        add(btnlogin);

        btnsignup = new JButton("Sign Up");
        btnsignup.setFont(font18);
        btnsignup.setBackground(red);
        btnsignup.setForeground(white);
        btnsignup.setBounds(720, 470, 120, 40);
        add(btnsignup);

        btnClose = new JButton(new ImageIcon(getClass().getResource("/img/close icon.png")));
        btnClose.setBounds(1088, 0, 50, 40);
        add(btnClose);

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/login page.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnClose.addActionListener(e -> dispose());
        btnsignup.addActionListener(e -> new SignUp().setVisible(true));
        btnlogin.addActionListener(e -> login());

        txtemail.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) btnlogin.doClick();
            }
        });
        txtpassword.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) btnlogin.doClick();
            }
        });
    }

    private void login() {
        try (PreparedStatement pst = c.prepareStatement("SELECT * FROM library.login WHERE userid=? AND password=?")) {
            pst.setString(1, txtemail.getText());
            pst.setString(2, new String(txtpassword.getPassword()));
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                dispose();
                if ("admin".equalsIgnoreCase(role)) new Home().setVisible(true);
                else new ClientHome().setVisible(true);
            } else JOptionPane.showMessageDialog(this, "Invalid ID or Password!");
        } catch (SQLException ex) {
            Logger.getLogger(SignIn.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignIn().setVisible(true));
    }
}
