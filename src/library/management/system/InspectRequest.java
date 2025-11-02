import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.logging.*;

public class InspectRequest extends JFrame {
    private JTextField txtBookId, txtBookName, txtStudentId, txtStudentName;
    private JButton btnSearch, btnApprove, btnDeny, btnClose;
    private Connection c = Connect.ConnectToDB();
    private PreparedStatement pst;
    private ResultSet rs;

    public InspectRequest() {
        setTitle("Inspect Request");
        setSize(800, 500);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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

        btnSearch = new JButton("Search");
        btnSearch.setBackground(red);
        btnSearch.setForeground(white);
        btnSearch.setBounds(480, 100, 100, 30);
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

        btnApprove = new JButton("Approve");
        btnApprove.setBackground(new Color(0, 153, 0));
        btnApprove.setForeground(white);
        btnApprove.setFont(font18);
        btnApprove.setBounds(180, 360, 130, 40);
        add(btnApprove);

        btnDeny = new JButton("Deny");
        btnDeny.setBackground(red);
        btnDeny.setForeground(white);
        btnDeny.setFont(font18);
        btnDeny.setBounds(340, 360, 130, 40);
        add(btnDeny);

        btnClose = new JButton("Close");
        btnClose.setBounds(500, 360, 130, 40);
        add(btnClose);

        btnSearch.addActionListener(e -> searchRequest());
        btnApprove.addActionListener(e -> approveRequest());
        btnDeny.addActionListener(e -> denyRequest());
        btnClose.addActionListener(e -> dispose());
    }

    private void searchRequest() {
        try {
            pst = c.prepareStatement("SELECT * FROM library.request WHERE book_id=?");
            pst.setString(1, txtBookId.getText());
            rs = pst.executeQuery();

            if (rs.next()) {
                txtBookName.setText(rs.getString("book_name"));
                txtStudentId.setText(rs.getString("student_id"));
                txtStudentName.setText(rs.getString("student_name"));
            } else {
                JOptionPane.showMessageDialog(this, "❌ No request found with this Book ID.");
            }
        } catch (SQLException ex) {
            Logger.getLogger(InspectRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void approveRequest() {
        try {
            // cập nhật trạng thái book
            PreparedStatement update = c.prepareStatement(
                "UPDATE library.book SET status='Issued', studentid=? WHERE id=?"
            );
            update.setString(1, txtStudentId.getText());
            update.setString(2, txtBookId.getText());
            update.executeUpdate();

            // xóa request
            PreparedStatement del = c.prepareStatement("DELETE FROM library.request WHERE book_id=?");
            del.setString(1, txtBookId.getText());
            del.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Request approved successfully!");
            clearFields();
        } catch (SQLException ex) {
            Logger.getLogger(InspectRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void denyRequest() {
        try {
            PreparedStatement del = c.prepareStatement("DELETE FROM library.request WHERE book_id=?");
            del.setString(1, txtBookId.getText());
            del.executeUpdate();

            JOptionPane.showMessageDialog(this, "❌ Request denied and removed.");
            clearFields();
        } catch (SQLException ex) {
            Logger.getLogger(InspectRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void clearFields() {
        txtBookId.setText("");
        txtBookName.setText("");
        txtStudentId.setText("");
        txtStudentName.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InspectRequest().setVisible(true));
    }
}
