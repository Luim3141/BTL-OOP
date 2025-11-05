import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.logging.*;

public class InspectRequest extends JFrame {
    private JTextField txtBookId, txtBookName, txtStudentId, txtStudentName, txtIssueDate, txtDueDate;
    private JButton btnSearch, btnApprove, btnDeny, btnClose;
    private Connection c = Connect.ConnectToDB();
    private PreparedStatement pst;
    private ResultSet rs;

    public InspectRequest() {
        setTitle("Inspect Book Request");
        setSize(900, 600);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color green = new Color(0, 153, 0);
        Color white = Color.WHITE;

        JLabel lblTitle = new JLabel("Inspect Book Request");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setBounds(300, 30, 350, 40);
        add(lblTitle);

        JLabel lblBookId = new JLabel("Book ID:");
        lblBookId.setFont(font18);
        lblBookId.setBounds(100, 100, 150, 30);
        add(lblBookId);

        txtBookId = new JTextField();
        txtBookId.setBounds(250, 100, 200, 30);
        add(txtBookId);

        btnSearch = new JButton("Search");
        btnSearch.setBackground(red);
        btnSearch.setForeground(white);
        btnSearch.setBounds(480, 100, 120, 30);
        add(btnSearch);

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
        txtStudentId.setEditable(false);
        add(txtStudentId);

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

        txtDueDate = new JTextField();
        txtDueDate.setBounds(250, 400, 200, 30);
        txtDueDate.setEditable(false);
        add(txtDueDate);

        btnApprove = new JButton("Approve");
        btnApprove.setFont(font18);
        btnApprove.setBackground(green);
        btnApprove.setForeground(white);
        btnApprove.setBounds(180, 470, 130, 40);
        add(btnApprove);

        btnDeny = new JButton("Deny");
        btnDeny.setFont(font18);
        btnDeny.setBackground(red);
        btnDeny.setForeground(white);
        btnDeny.setBounds(340, 470, 130, 40);
        add(btnDeny);

        btnClose = new JButton("Close");
        btnClose.setFont(font18);
        btnClose.setBounds(500, 470, 130, 40);
        add(btnClose);

        // Event Listeners
        btnSearch.addActionListener(e -> searchRequest());
        btnApprove.addActionListener(e -> approveRequest());
        btnDeny.addActionListener(e -> denyRequest());
        btnClose.addActionListener(e -> dispose());
    }

    // 🔍 Search request by Book ID
    private void searchRequest() {
        try {
            pst = c.prepareStatement("SELECT * FROM library.request WHERE book_id=?");
            pst.setString(1, txtBookId.getText());
            rs = pst.executeQuery();

            if (rs.next()) {
                txtBookName.setText(rs.getString("book_name"));
                txtStudentId.setText(rs.getString("student_id"));
                txtStudentName.setText(rs.getString("student_name"));
                txtIssueDate.setText(rs.getString("issue_date"));
                txtDueDate.setText(rs.getString("due_date"));
                JOptionPane.showMessageDialog(this, "✅ Request found!");
            } else {
                JOptionPane.showMessageDialog(this, "❌ No request found with this Book ID.");
                clearFields();
            }
        } catch (SQLException ex) {
            Logger.getLogger(InspectRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // ✅ Approve the request
    private void approveRequest() {
        try {
            // Lấy dữ liệu request hiện tại
            PreparedStatement sel = c.prepareStatement(
                    "SELECT * FROM library.request WHERE book_id=?");
            sel.setString(1, txtBookId.getText());
            ResultSet rs = sel.executeQuery();

            if (rs.next()) {
                String studentId = rs.getString("student_id");
                String issueDate = rs.getString("issue_date");
                String dueDate = rs.getString("due_date");

                // Cập nhật thông tin sang bảng book
                PreparedStatement update = c.prepareStatement(
                        "UPDATE library.book SET status='Issued', studentid=?, issuedate=?, duedate=? WHERE id=?");
                update.setString(1, studentId);
                update.setString(2, issueDate);
                update.setString(3, dueDate);
                update.setString(4, txtBookId.getText());
                update.executeUpdate();

                // Xóa request khỏi bảng request
                PreparedStatement del = c.prepareStatement(
                        "DELETE FROM library.request WHERE book_id=?");
                del.setString(1, txtBookId.getText());
                del.executeUpdate();

                JOptionPane.showMessageDialog(this, "✅ Request approved successfully!");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "❌ No matching request found!");
            }

        } catch (SQLException ex) {
            Logger.getLogger(InspectRequest.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "⚠️ Database error: " + ex.getMessage());
        }
    }

    // ❌ Deny the request
    private void denyRequest() {
        try {
            PreparedStatement del = c.prepareStatement(
                    "DELETE FROM library.request WHERE book_id=?");
            del.setString(1, txtBookId.getText());
            int rows = del.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "❌ Request denied and removed.");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "⚠️ No request found to delete.");
            }
        } catch (SQLException ex) {
            Logger.getLogger(InspectRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // 🧹 Clear all fields
    private void clearFields() {
        txtBookId.setText("");
        txtBookName.setText("");
        txtStudentId.setText("");
        txtStudentName.setText("");
        txtIssueDate.setText("");
        txtDueDate.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InspectRequest().setVisible(true));
    }
}
