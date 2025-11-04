import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IssueBook extends JFrame {
    private JLabel lblTitle, lblBookID, lblStudentID, lblBookName, lblAuthor, lblIssueDate, lblDueDate, lblBg;
    private JTextField txtid, txtstudentid, txtbookname, txtauthor, txtissuedate, txtduedate;
    private JButton btnIssue, btnSearch, btnClose;

    PreparedStatement pst;
    ResultSet rs;
    Connection c = Connect.ConnectToDB();

    public IssueBook() {
        // ==== FRAME SETTINGS ====
        setTitle("Issue Book");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // ==== FONT & COLOR ====
        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Font font24 = new Font("Segoe UI", Font.BOLD, 24);
        Color red = new Color(204, 0, 0);
        Color white = new Color(242, 242, 242);

        // ==== TITLE ====
        lblTitle = new JLabel("Issue Book");
        lblTitle.setFont(font24);
        lblTitle.setForeground(red);
        lblTitle.setIcon(new ImageIcon(getClass().getResource("/img/isue.jpg")));
        lblTitle.setBounds(40, 60, 200, 70);
        add(lblTitle);

        // ==== LABELS ====
        lblBookID = new JLabel("Book ID");
        lblBookID.setFont(font18);
        lblBookID.setBounds(140, 180, 250, 40);
        add(lblBookID);

        lblStudentID = new JLabel("Student ID");
        lblStudentID.setFont(font18);
        lblStudentID.setBounds(140, 250, 240, 40);
        add(lblStudentID);

        lblBookName = new JLabel("Book Name");
        lblBookName.setFont(font18);
        lblBookName.setBounds(140, 320, 250, 40);
        add(lblBookName);

        lblAuthor = new JLabel("Author");
        lblAuthor.setFont(font18);
        lblAuthor.setBounds(140, 390, 250, 40);
        add(lblAuthor);

        lblIssueDate = new JLabel("Issue Date");
        lblIssueDate.setFont(font18);
        lblIssueDate.setBounds(140, 460, 240, 40);
        add(lblIssueDate);

        lblDueDate = new JLabel("Due Date");
        lblDueDate.setFont(font18);
        lblDueDate.setBounds(140, 530, 240, 40);
        add(lblDueDate);

        // ==== TEXTFIELDS ====
        txtid = new JTextField();
        txtid.setFont(font18);
        txtid.setBounds(320, 180, 350, 40);
        add(txtid);

        txtstudentid = new JTextField();
        txtstudentid.setFont(font18);
        txtstudentid.setBounds(320, 250, 350, 40);
        add(txtstudentid);

        txtbookname = new JTextField();
        txtbookname.setFont(font18);
        txtbookname.setEditable(false);
        txtbookname.setBounds(320, 320, 350, 40);
        add(txtbookname);

        txtauthor = new JTextField();
        txtauthor.setFont(font18);
        txtauthor.setEditable(false);
        txtauthor.setBounds(320, 390, 350, 40);
        add(txtauthor);

        txtissuedate = new JTextField();
        txtissuedate.setFont(font18);
        txtissuedate.setEditable(false);
        txtissuedate.setBounds(320, 460, 350, 40);
        add(txtissuedate);

        txtduedate = new JTextField();
        txtduedate.setFont(font18);
        txtduedate.setBounds(320, 530, 350, 40);
        add(txtduedate);

        // ==== BUTTONS ====
        btnIssue = new JButton("Issue Book");
        btnIssue.setFont(font18);
        btnIssue.setBackground(red);
        btnIssue.setForeground(white);
        btnIssue.setBounds(260, 620, 150, 45);
        add(btnIssue);

        btnSearch = new JButton("Search");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSearch.setBackground(red);
        btnSearch.setForeground(white);
        btnSearch.setBounds(690, 185, 100, 35);
        add(btnSearch);

        btnClose = new JButton(new ImageIcon(getClass().getResource("/img/close icon.png")));
        btnClose.setBounds(1090, 0, 51, 40);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setContentAreaFilled(false);
        add(btnClose);

        // ==== BACKGROUND ====
        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        // ==== CURRENT DATE ====
        SimpleDateFormat dat = new SimpleDateFormat("dd/MM/yyyy");
        txtissuedate.setText(dat.format(new Date()));

        // ==== ACTION LISTENERS ====
        btnClose.addActionListener(e -> dispose());
        btnIssue.addActionListener(e -> issueBook());
        btnSearch.addActionListener(e -> searchBook());
    }

    private void searchBook() {
        String id = txtid.getText();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Book ID!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            pst = c.prepareStatement("SELECT * FROM library.book WHERE id=?");
            pst.setString(1, id);
            rs = pst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "No book found with this ID!", "Error", JOptionPane.ERROR_MESSAGE);
                clearFields();
                return;
            }

            // Check status
            if ("Issued".equalsIgnoreCase(rs.getString("status"))) {
                JOptionPane.showMessageDialog(this, "This book is already issued!", "Warning", JOptionPane.WARNING_MESSAGE);
                clearFields();
                return;
            }

            txtbookname.setText(rs.getString("name"));
            txtauthor.setText(rs.getString("author"));

        } catch (SQLException ex) {
            Logger.getLogger(IssueBook.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Error fetching book details!");
        }
    }

    private void issueBook() {
        if (txtid.getText().isEmpty() || txtstudentid.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill Book ID and Student ID!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check student exists
        try {
            pst = c.prepareStatement("SELECT * FROM library.student WHERE id=?");
            pst.setString(1, txtstudentid.getText());
            rs = pst.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "No student found with this ID!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (SQLException ex) {
            Logger.getLogger(IssueBook.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            pst = c.prepareStatement("UPDATE library.book SET status='Issued', issuedate=?, duedate=?, studentid=? WHERE id=?");
            pst.setString(1, txtissuedate.getText());
            pst.setString(2, txtduedate.getText());
            pst.setString(3, txtstudentid.getText());
            pst.setString(4, txtid.getText());
            int updated = pst.executeUpdate();

            if (updated > 0) {
                JOptionPane.showMessageDialog(this, "Book issued successfully!");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to issue book!");
            }

        } catch (SQLException ex) {
            Logger.getLogger(IssueBook.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void clearFields() {
        txtbookname.setText("");
        txtauthor.setText("");
        txtduedate.setText("");
        txtissuedate.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        txtstudentid.setText("");
        txtid.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IssueBook().setVisible(true));
    }
}
