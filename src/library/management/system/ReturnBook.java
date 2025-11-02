import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReturnBook extends JFrame {
    // ==== GUI components ====
    private JTextField txtbookid, txtbookname, txtduedate, txtissuedate, txtstudentid, txtstudentname;
    private JButton btnReturn, btnSearch, btnClose;
    private JLabel lblBookID, lblStudentID, lblStudentName, lblBookName, lblIssueDate, lblDueDate, lblTitle, lblBg;

    // ==== Database ====
    private Connection c = Connect.ConnectToDB();
    private PreparedStatement pst;
    private ResultSet rs;

    // ==== Constructor ====
    public ReturnBook() {
        // --- Frame settings ---
        setTitle("Return Book");
        setSize(1140, 770);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setLocationRelativeTo(null);

        // --- Fonts and colors ---
        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Font font14 = new Font("Segoe UI", Font.BOLD, 14);
        Color red = new Color(204, 0, 0);
        Color white = new Color(242, 242, 242);

        // --- Title label ---
        lblTitle = new JLabel("Return Book");
        lblTitle.setFont(font18);
        lblTitle.setForeground(white);
        lblTitle.setIcon(new ImageIcon(getClass().getResource("/img/isue.jpg")));
        lblTitle.setBounds(20, 40, 240, 60);
        add(lblTitle);

        // --- Labels ---
        lblBookID = new JLabel("Book ID");
        lblBookID.setFont(font18);
        lblBookID.setBounds(120, 180, 270, 50);
        add(lblBookID);

        lblStudentID = new JLabel("Student ID");
        lblStudentID.setFont(font18);
        lblStudentID.setBounds(120, 260, 270, 50);
        add(lblStudentID);

        lblStudentName = new JLabel("Student Name");
        lblStudentName.setFont(font18);
        lblStudentName.setBounds(120, 340, 220, 50);
        add(lblStudentName);

        lblBookName = new JLabel("Book Name");
        lblBookName.setFont(font18);
        lblBookName.setBounds(120, 410, 220, 50);
        add(lblBookName);

        lblIssueDate = new JLabel("Issue Date");
        lblIssueDate.setFont(font18);
        lblIssueDate.setBounds(120, 490, 270, 50);
        add(lblIssueDate);

        lblDueDate = new JLabel("Due Date");
        lblDueDate.setFont(font18);
        lblDueDate.setBounds(120, 560, 270, 50);
        add(lblDueDate);

        // --- Text fields ---
        txtbookid = new JTextField();
        txtbookid.setFont(font14);
        txtbookid.setBounds(340, 180, 350, 40);
        add(txtbookid);

        txtstudentid = new JTextField();
        txtstudentid.setFont(font14);
        txtstudentid.setBounds(340, 260, 350, 40);
        add(txtstudentid);

        txtstudentname = new JTextField();
        txtstudentname.setFont(font14);
        txtstudentname.setBounds(340, 340, 350, 40);
        txtstudentname.setEditable(false);
        add(txtstudentname);

        txtbookname = new JTextField();
        txtbookname.setFont(font14);
        txtbookname.setBounds(340, 410, 350, 40);
        txtbookname.setEditable(false);
        add(txtbookname);

        txtissuedate = new JTextField();
        txtissuedate.setFont(font14);
        txtissuedate.setBounds(340, 480, 350, 40);
        txtissuedate.setEditable(false);
        add(txtissuedate);

        txtduedate = new JTextField();
        txtduedate.setFont(font14);
        txtduedate.setBounds(340, 560, 350, 40);
        txtduedate.setEditable(false);
        add(txtduedate);

        // --- Buttons ---
        btnReturn = new JButton("Return");
        btnReturn.setFont(font18);
        btnReturn.setBackground(red);
        btnReturn.setForeground(white);
        btnReturn.setBounds(330, 640, 130, 40);
        add(btnReturn);

        // 🔧 moved Search button next to Book ID
        btnSearch = new JButton("Search");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnSearch.setBackground(red);
        btnSearch.setForeground(white);
        btnSearch.setBounds(710, 185, 100, 30); // was 696,267
        add(btnSearch);

        btnClose = new JButton(new ImageIcon(getClass().getResource("/img/close icon.png")));
        btnClose.setBounds(1088, 0, 50, 40);
        add(btnClose);

        // --- Background ---
        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        // --- Listeners ---
        btnClose.addActionListener(e -> dispose());
        btnSearch.addActionListener(e -> searchBook());
        btnReturn.addActionListener(e -> returnBook());
    }

    // ==== Clear all fields ====
    private void clear() {
        txtbookid.setText("");
        txtbookname.setText("");
        txtduedate.setText("");
        txtissuedate.setText("");
        txtstudentid.setText("");
        txtstudentname.setText("");
    }

    // ==== Search for issued book info ====
    private void searchBook() {
        try {
            pst = c.prepareStatement("SELECT * FROM library.book WHERE id=?");
            pst.setString(1, txtbookid.getText());
            rs = pst.executeQuery();

            if (rs.next()) {
                txtbookname.setText(rs.getString("name"));
                txtissuedate.setText(rs.getString("issuedate"));
                txtduedate.setText(rs.getString("duedate"));
                txtstudentid.setText(rs.getString("studentid"));
            } else {
                JOptionPane.showMessageDialog(this, "Book not found! Please enter valid Book ID.");
                return;
            }

            if (!txtstudentid.getText().isEmpty()) {
                pst = c.prepareStatement("SELECT * FROM library.student WHERE id=?");
                pst.setString(1, txtstudentid.getText());
                rs = pst.executeQuery();
                if (rs.next()) {
                    txtstudentname.setText(rs.getString("name"));
                } else {
                    txtstudentname.setText("Unknown Student");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(ReturnBook.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    // ==== Return the book ====
    private void returnBook() {
        if (txtbookid.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Book ID and search again.");
            txtbookid.requestFocus();
            return;
        }

        try {
            pst = c.prepareStatement(
                "UPDATE library.book SET status='NotIssued', issuedate=NULL, duedate=NULL, studentid=NULL WHERE id=?"
            );
            pst.setString(1, txtbookid.getText());
            int updated = pst.executeUpdate();

            if (updated > 0) {
                JOptionPane.showMessageDialog(this, "Book returned successfully!");
                clear();
            } else {
                JOptionPane.showMessageDialog(this, "Book not found or already returned.");
            }
        } catch (SQLException ex) {
            Logger.getLogger(ReturnBook.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    // ==== Main entry point ====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ReturnBook().setVisible(true));
    }
}
