import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.logging.*;

public class IssueClientRQ extends JFrame {
    private JTextField txtBookId, txtBookName, txtStudentId, txtStudentName;
    private JButton btnRequest, btnSearchBook, btnSearchStudent, btnClose;
    private Connection c = Connect.ConnectToDB();
    private PreparedStatement pst;
    private ResultSet rs;
    private boolean validBook = false, validStudent = false;

    public IssueClientRQ() {
        setTitle("Request Book (Client)");
        setSize(900, 600);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

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

        btnRequest = new JButton("Send Request");
        btnRequest.setBackground(red);
        btnRequest.setForeground(white);
        btnRequest.setFont(font18);
        btnRequest.setBounds(250, 360, 200, 40);
        add(btnRequest);

        btnClose = new JButton("Close");
        btnClose.setBounds(500, 360, 120, 40);
        add(btnClose);

        btnSearchBook.addActionListener(e -> searchBook());
        btnSearchStudent.addActionListener(e -> searchStudent());
        btnRequest.addActionListener(e -> sendRequest());
        btnClose.addActionListener(e -> dispose());
    }

    private void searchBook() {
        validBook = false;
        try {
            pst = c.prepareStatement("SELECT * FROM library.book WHERE id=?");
            pst.setString(1, txtBookId.getText());
            rs = pst.executeQuery();

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

            validBook = true;
            JOptionPane.showMessageDialog(this, "✅ Book found and available for request.");

        } catch (SQLException ex) {
            Logger.getLogger(IssueClientRQ.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void searchStudent() {
        validStudent = false;
        try {
            pst = c.prepareStatement("SELECT * FROM library.student WHERE id=?");
            pst.setString(1, txtStudentId.getText());
            rs = pst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "❌ Invalid Student ID. Please recheck.");
                return;
            }

            txtStudentName.setText(rs.getString("name"));
            validStudent = true;
            JOptionPane.showMessageDialog(this, "✅ Student verified.");
        } catch (SQLException ex) {
            Logger.getLogger(IssueClientRQ.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendRequest() {
        if (!validBook || !validStudent) {
            JOptionPane.showMessageDialog(this, "⚠️ Please verify both Book and Student IDs first.");
            return;
        }

        try {
            pst = c.prepareStatement("SELECT * FROM library.request WHERE book_id=?");
            pst.setString(1, txtBookId.getText());
            rs = pst.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "⚠️ This book already has a pending request.");
                return;
            }

            pst = c.prepareStatement("INSERT INTO library.request (book_id, book_name, student_id, student_name) VALUES (?, ?, ?, ?)");
            pst.setString(1, txtBookId.getText());
            pst.setString(2, txtBookName.getText());
            pst.setString(3, txtStudentId.getText());
            pst.setString(4, txtStudentName.getText());
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Request submitted successfully!");
            clearFields();

        } catch (SQLException ex) {
            Logger.getLogger(IssueClientRQ.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void clearFields() {
        txtBookId.setText("");
        txtBookName.setText("");
        txtStudentId.setText("");
        txtStudentName.setText("");
        validBook = false;
        validStudent = false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IssueClientRQ().setVisible(true));
    }
}
