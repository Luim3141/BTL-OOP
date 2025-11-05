import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Home extends JFrame {

    private JButton btnAddBook, btnIssueBook, btnReturnBook, btnBookList, btnDeleteBook,
                    btnManageStudent, btnAccountManage, btnIssueRequest, btnLogout;
    private JLabel lblBg;

    public Home() {
        // ==== FRAME SETTINGS ====
        setTitle("Admin Home");
        setSize(1200, 800);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = Color.WHITE;

        // ==== BUTTONS ====
        btnAddBook = createButton("Add Book", "/img/addbook.png", red, white, font18);
        btnIssueBook = createButton("Issue Book", "/img/issuebook.png", red, white, font18);
        btnReturnBook = createButton("Return Book", "/img/returnbook.png", red, white, font18);
        btnBookList = createButton("Book List", "/img/booklist.png", red, white, font18);
        btnDeleteBook = createButton("Delete Book", "/img/deletebook.png", red, white, font18);
        btnManageStudent = createButton("Manage Student", "/img/managestudent.png", red, white, font18);
        btnAccountManage = createButton("Account Manage", "/img/account.png", red, white, font18);
        btnIssueRequest = createButton("Issue Request", "/img/request.png", red, white, font18);
        btnLogout = createButton("Logout", "/img/logout.png", red, white, font18);

        // ==== LAYOUT ====
        int startX = 100, startY = 150, gapX = 260, gapY = 110, width = 220, height = 60;
        btnAddBook.setBounds(startX, startY, width, height);
        btnIssueBook.setBounds(startX + gapX, startY, width, height);
        btnReturnBook.setBounds(startX + 2 * gapX, startY, width, height);

        btnBookList.setBounds(startX, startY + gapY, width, height);
        btnDeleteBook.setBounds(startX + gapX, startY + gapY, width, height);
        btnManageStudent.setBounds(startX + 2 * gapX, startY + gapY, width, height);

        btnAccountManage.setBounds(startX, startY + 2 * gapY, width, height);
        btnIssueRequest.setBounds(startX + gapX, startY + 2 * gapY, width, height);
        btnLogout.setBounds(1000, 680, 150, 50);

        add(btnAddBook);
        add(btnIssueBook);
        add(btnReturnBook);
        add(btnBookList);
        add(btnDeleteBook);
        add(btnManageStudent);
        add(btnAccountManage);
        add(btnIssueRequest);
        add(btnLogout);

        // ==== BACKGROUND ====
        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/home page.jpg")));
        lblBg.setBounds(0, 0, 1200, 800);
        add(lblBg);

        // ==== ACTION LISTENERS ====
        btnAddBook.addActionListener(e -> new AddBook().setVisible(true));
        btnIssueBook.addActionListener(e -> new IssueBook().setVisible(true));
        btnReturnBook.addActionListener(e -> new ReturnBook().setVisible(true));
        btnBookList.addActionListener(e -> new ListBooks().setVisible(true));
        btnDeleteBook.addActionListener(e -> new DeleteBook().setVisible(true));
        btnManageStudent.addActionListener(e -> new ManageStudent().setVisible(true));
        btnAccountManage.addActionListener(e -> new AccountManage().setVisible(true));
        btnIssueRequest.addActionListener(e -> new IssueAdminRQ().setVisible(true));
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
        SwingUtilities.invokeLater(() -> new Home().setVisible(true));
    }
}
