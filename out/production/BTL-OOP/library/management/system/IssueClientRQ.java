import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class IssueClientRQ extends JFrame {
    private JTextField txtBookId, txtBookName, txtStudentId, txtStudentName, txtIssueDate, txtDueDate;
    private JButton btnRequest, btnSearchBook, btnSearchStudent, btnClose;
    private Connection c;
    private boolean validBook = false, validStudent = false;

    public IssueClientRQ() {
        setTitle("Request Book (Client)");
        setSize(900, 650);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // open connection
        c = Connect.ConnectToDB();

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = Color.WHITE;

        JLabel lblBookId = new JLabel("Book ID:");
        lblBookId.setFont(font18);
        lblBookId.setBounds(100, 100, 150, 30);
        add(lblBookId);

        txtBookId = new JTextField();
        txtBookId.setBounds(250, 100, 200, 30);
        add(txtBookId);

        btnSearchBook = new JButton("Search Book");
        btnSearchBook.setBackground(red);
        btnSearchBook.setForeground(white);
        btnSearchBook.setBounds(480, 100, 150, 30);
        add(btnSearchBook);

        JLabel lblBookName = new JLabel("Book Name:");
        lblBookName.setFont(font18);
        lblBookName.setBounds(100, 160, 150, 30);
        add(lblBookName);

        txtBookName = new JTextField();
        txtBookName.setBounds(250, 160, 200, 30);
        txtBookName.setEditable(false);
        add(txtBookName);

        JLabel lblStudentId = new JLabel("Student ID:");
        lblStudentId.setFont(font18);
        lblStudentId.setBounds(100, 220, 150, 30);
        add(lblStudentId);

        txtStudentId = new JTextField();
        txtStudentId.setBounds(250, 220, 200, 30);
        add(txtStudentId);

        btnSearchStudent = new JButton("Search Student");
        btnSearchStudent.setBackground(red);
        btnSearchStudent.setForeground(white);
        btnSearchStudent.setBounds(480, 220, 150, 30);
        add(btnSearchStudent);

        JLabel lblStudentName = new JLabel("Student Name:");
        lblStudentName.setFont(font18);
        lblStudentName.setBounds(100, 280, 150, 30);
        add(lblStudentName);

        txtStudentName = new JTextField();
        txtStudentName.setBounds(250, 280, 200, 30);
        txtStudentName.setEditable(false);
        add(txtStudentName);

        JLabel lblIssueDate = new JLabel("Issue Date:");
        lblIssueDate.setFont(font18);
        lblIssueDate.setBounds(100, 340, 150, 30);
        add(lblIssueDate);

        txtIssueDate = new JTextField();
        txtIssueDate.setBounds(250, 340, 200, 30);
        txtIssueDate.setEditable(false);
        add(txtIssueDate);

        JLabel lblDueDate = new JLabel("Due Date:");
        lblDueDate.setFont(font18);
        lblDueDate.setBounds(100, 400, 150, 30);
        add(lblDueDate);

        txtDueDate = new JTextField("dd/MM/yyyy");
        txtDueDate.setBounds(250, 400, 200, 30);
        add(txtDueDate);

        btnRequest = new JButton("Send Request");
        btnRequest.setBackground(red);
        btnRequest.setForeground(Color.WHITE);
        btnRequest.setFont(font18);
        btnRequest.setBounds(250, 470, 200, 40);
        add(btnRequest);

        btnClose = new JButton("Close");
        btnClose.setBounds(500, 470, 120, 40);
        add(btnClose);

        // Listeners
        btnSearchBook.addActionListener(e -> searchBook());
        btnSearchStudent.addActionListener(e -> searchStudent());
        btnRequest.addActionListener(e -> sendRequest());
        btnClose.addActionListener(e -> dispose());
    }

    private Connection ensureConnection() throws SQLException {
        if (c == null || c.isClosed()) {
            c = Connect.ConnectToDB();
        }
        return c;
    }

    // 🔍 Search Book: also auto-fill issue_date = today if available
    private void searchBook() {
        validBook = false;
        try (Connection conn = ensureConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT * FROM library.book WHERE id=?")) {

            pst.setString(1, txtBookId.getText().trim());
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "❌ No book found with this ID.");
                    return;
                }

                String name = rs.getString("name");
                String status = rs.getString("status");
                txtBookName.setText(name);

                if ("Issued".equalsIgnoreCase(status)) {
                    JOptionPane.showMessageDialog(this, "⚠️ This book is already issued. Cannot request now.");
                    return;
                }

                // Auto-fill today's date
                String today = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
                txtIssueDate.setText(today);

                validBook = true;
                JOptionPane.showMessageDialog(this, "✅ Book found and available. Issue date auto-filled.");
            }
        } catch (SQLException ex) {
            Logger.getLogger(IssueClientRQ.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "DB error (searchBook): " + ex.getMessage());
        }
    }

    // 🔍 Search Student
    private void searchStudent() {
        validStudent = false;
        try (Connection conn = ensureConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT * FROM library.student WHERE id=?")) {

            pst.setString(1, txtStudentId.getText().trim());
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "❌ Invalid Student ID. Please recheck.");
                    return;
                }
                txtStudentName.setText(rs.getString("name"));
                validStudent = true;
                JOptionPane.showMessageDialog(this, "✅ Student verified.");
            }
        } catch (SQLException ex) {
            Logger.getLogger(IssueClientRQ.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "DB error (searchStudent): " + ex.getMessage());
        }
    }

    // 📤 Send Request (with full error messages + transaction)
    private void sendRequest() {
        // Guard conditions
        if (!validBook || !validStudent) {
            JOptionPane.showMessageDialog(this, "⚠️ Please press both Search buttons to verify Book & Student first.");
            return;
        }
        if (txtDueDate.getText().trim().isEmpty() || "dd/MM/yyyy".equals(txtDueDate.getText().trim())) {
            JOptionPane.showMessageDialog(this, "⚠️ Please enter Due Date (dd/MM/yyyy).");
            return;
        }
        if (txtIssueDate.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ Issue Date missing. Please search book again.");
            return;
        }

        try (Connection conn = ensureConnection()) {
            // quick debug of active schema
            try (Statement st = conn.createStatement();
                 ResultSet rdb = st.executeQuery("SELECT DATABASE()")) {
                if (rdb.next()) {
                    System.out.println("Connected schema: " + rdb.getString(1));
                }
            }

            conn.setAutoCommit(false);

            // 1) check duplicate request
            try (PreparedStatement chk = conn.prepareStatement(
                    "SELECT 1 FROM library.request WHERE book_id=?")) {
                chk.setString(1, txtBookId.getText().trim());
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        JOptionPane.showMessageDialog(this, "⚠️ This book already has a pending request.");
                        return;
                    }
                }
            }

            // 2) insert request (columns must match your table)
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO library.request (book_id, book_name, student_id, student_name, issue_date, due_date) " +
                            "VALUES (?, ?, ?, ?, ?, ?)")) {
                ins.setString(1, txtBookId.getText().trim());
                ins.setString(2, txtBookName.getText().trim());
                ins.setString(3, txtStudentId.getText().trim());
                ins.setString(4, txtStudentName.getText().trim());
                ins.setString(5, txtIssueDate.getText().trim());
                ins.setString(6, txtDueDate.getText().trim());
                int rows = ins.executeUpdate();
                if (rows <= 0) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(this, "❌ Insert failed (0 rows).");
                    return;
                }
            }

            conn.commit();
            conn.setAutoCommit(true);
            JOptionPane.showMessageDialog(this, "✅ Request submitted successfully!");
            clearFields();

        } catch (SQLException ex) {
            Logger.getLogger(IssueClientRQ.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "DB error (sendRequest): " + ex.getMessage());
        }
    }

    private void clearFields() {
        txtBookId.setText("");
        txtBookName.setText("");
        txtStudentId.setText("");
        txtStudentName.setText("");
        txtIssueDate.setText("");
        txtDueDate.setText("dd/MM/yyyy");
        validBook = false;
        validStudent = false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IssueClientRQ().setVisible(true));
    }
}
