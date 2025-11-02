import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ClientHome extends JFrame {

    private JButton btnBookList, btnIssueBook, btnReturnBook, btnLogout;
    private JLabel lblBg;

    public ClientHome() {
        // ==== FRAME SETTINGS ====
        setTitle("Client Home");
        setSize(1200, 800);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = Color.WHITE;

        // ==== BUTTONS ====
        btnBookList = createButton("Book List", "/img/booklist.png", red, white, font18);
        btnIssueBook = createButton("Request Book", "/img/issuebook.png", red, white, font18);
        btnReturnBook = createButton("Return Book", "/img/returnbook.png", red, white, font18);
        btnLogout = createButton("Logout", "/img/logout.png", red, white, font18);

        // ==== LAYOUT ====
        int startX = 250, startY = 200, gapX = 260, width = 220, height = 60;
        btnBookList.setBounds(startX, startY, width, height);
        btnIssueBook.setBounds(startX + gapX, startY, width, height);
        btnReturnBook.setBounds(startX + 2 * gapX, startY, width, height);
        btnLogout.setBounds(1000, 680, 150, 50);

        add(btnBookList);
        add(btnIssueBook);
        add(btnReturnBook);
        add(btnLogout);

        // ==== BACKGROUND ====
        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/home page.jpg")));
        lblBg.setBounds(0, 0, 1200, 800);
        add(lblBg);

        // ==== ACTIONS ====
        btnBookList.addActionListener(e -> new ListBooks().setVisible(true));
        btnIssueBook.addActionListener(e -> new IssueClientRQ().setVisible(true)); // ✅ khác với admin
        btnReturnBook.addActionListener(e -> new ReturnBook().setVisible(true));
        btnLogout.addActionListener(e -> {
            dispose();
            new SignIn().setVisible(true);
        });
    }

    private JButton createButton(String text, String iconPath, Color bg, Color fg, Font font) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10);

        try {
            java.net.URL location = getClass().getResource(iconPath);
            if (location != null) {
                ImageIcon icon = new ImageIcon(location);
                Image scaledImg = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(scaledImg));
            } else {
                System.err.println("⚠️ Missing icon: " + iconPath);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error loading icon: " + iconPath);
        }

        Color hover = new Color(Math.max(bg.getRed() - 40, 0), 0, 0);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { button.setBackground(hover); }
            public void mouseExited(MouseEvent evt) { button.setBackground(bg); }
        });

        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientHome().setVisible(true));
    }
}
