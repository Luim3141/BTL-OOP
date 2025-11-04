import javax.swing.*;
import java.awt.*;

public class AccountManage extends JFrame {
    private JButton btnAddAccount, btnAdjustAccount, btnAccountDetail, btnClose;
    private JLabel lblBg;

    public AccountManage() {
        setTitle("Account Management");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);

        JLabel lblTitle = new JLabel("Account Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(red);
        lblTitle.setBounds(420, 70, 300, 40);
        add(lblTitle);

        btnAddAccount = new JButton("Add Account");
        btnAdjustAccount = new JButton("Adjust Account");
        btnAccountDetail = new JButton("Account Detail");
        btnClose = new JButton("Close");

        JButton[] buttons = {btnAddAccount, btnAdjustAccount, btnAccountDetail, btnClose};
        int y = 200;
        for (JButton b : buttons) {
            b.setFont(font18);
            b.setBackground(red);
            b.setForeground(white);
            b.setBounds(420, y, 300, 60);
            add(b);
            y += 100;
        }

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnAddAccount.addActionListener(e -> new AddAccount().setVisible(true));
        btnAdjustAccount.addActionListener(e -> new AdjustAccount().setVisible(true));
        btnAccountDetail.addActionListener(e -> new AccountDetail().setVisible(true));
        btnClose.addActionListener(e -> dispose());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AccountManage().setVisible(true));
    }
}
