import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class DeleteStudent extends JFrame {
    private JTextField txtid, txtname, txtcourse, txtbranch, txtsemester, txtgender, txtclass, txtemail, txtphone, txtaddress;
    private JButton btnSearch, btnDelete, btnClose;
    private JLabel lblBg;
    private boolean canDelete = false; // Cờ kiểm tra xem có được phép xóa không
    Connection c = Connect.ConnectToDB();

    public DeleteStudent() {
        setTitle("Delete Student");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);

        JLabel lblTitle = new JLabel("Delete Student");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(white);
        lblTitle.setBounds(440, 60, 300, 40);
        add(lblTitle);

        String[] labels = {"ID", "Name", "Course", "Branch", "Semester", "Gender", "Class", "Email", "Phone", "Address"};
        JTextField[] fields = new JTextField[labels.length];
        int y = 140;
        for (int i = 0; i < labels.length; i++) {
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(font18);
            lbl.setForeground(white);
            lbl.setBounds(200, y, 150, 40);
            add(lbl);

            fields[i] = new JTextField();
            fields[i].setFont(font18);
            fields[i].setBounds(360, y, 350, 40);
            add(fields[i]);
            y += 50;
        }

        txtid = fields[0];
        txtname = fields[1];
        txtcourse = fields[2];
        txtbranch = fields[3];
        txtsemester = fields[4];
        txtgender = fields[5];
        txtclass = fields[6];
        txtemail = fields[7];
        txtphone = fields[8];
        txtaddress = fields[9];

        btnSearch = new JButton("Search");
        btnSearch.setFont(font18);
        btnSearch.setBackground(red);
        btnSearch.setForeground(white);
        btnSearch.setBounds(730, 140, 120, 40);
        add(btnSearch);

        btnDelete = new JButton("Delete");
        btnDelete.setFont(font18);
        btnDelete.setBackground(red);
        btnDelete.setForeground(white);
        btnDelete.setBounds(360, 660, 120, 40);
        add(btnDelete);

        btnClose = new JButton("Close");
        btnClose.setFont(font18);
        btnClose.setBackground(red);
        btnClose.setForeground(white);
        btnClose.setBounds(500, 660, 120, 40);
        add(btnClose);

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnSearch.addActionListener(e -> searchStudent());
        btnDelete.addActionListener(e -> deleteStudent());
        btnClose.addActionListener(e -> dispose());
    }

    private void searchStudent() {
        clearFields();
        canDelete = false;

        try {
            PreparedStatement pst = c.prepareStatement("SELECT * FROM library.student WHERE id=?");
            pst.setString(1, txtid.getText());
            ResultSet rs = pst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "❌ No student found with ID: " + txtid.getText());
                return;
            }

            // kiểm tra xem có đang mượn sách không
            pst = c.prepareStatement("SELECT * FROM library.book WHERE studentid=? AND status='Issued'");
            pst.setString(1, txtid.getText());
            ResultSet rsBook = pst.executeQuery();

            if (rsBook.next()) {
                JOptionPane.showMessageDialog(this,
                        "⚠️ This student currently has an issued book.\nYou cannot delete until all books are returned.");
                return; // Dừng, không điền thông tin
            }

            // nếu không mượn sách -> điền thông tin
            canDelete = true;
            txtname.setText(rs.getString("name"));
            txtcourse.setText(rs.getString("course"));
            txtbranch.setText(rs.getString("branch"));
            txtsemester.setText(rs.getString("semester"));
            txtgender.setText(rs.getString("gender"));
            txtclass.setText(rs.getString("class"));
            txtemail.setText(rs.getString("email"));
            txtphone.setText(rs.getString("phone"));
            txtaddress.setText(rs.getString("address"));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error searching student: " + ex.getMessage());
        }
    }

    private void deleteStudent() {
        if (!canDelete) {
            JOptionPane.showMessageDialog(this,
                    "❌ Cannot delete this student.\nEither no data loaded or student has an issued book.");
            return;
        }

        try (PreparedStatement pst = c.prepareStatement("DELETE FROM library.student WHERE id=?")) {
            pst.setString(1, txtid.getText());
            int rows = pst.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "✅ Student deleted successfully!");
                clearFields();
                canDelete = false;
            } else {
                JOptionPane.showMessageDialog(this, "⚠️ Delete failed!");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting: " + ex.getMessage());
        }
    }

    private void clearFields() {
        txtname.setText("");
        txtcourse.setText("");
        txtbranch.setText("");
        txtsemester.setText("");
        txtgender.setText("");
        txtclass.setText("");
        txtemail.setText("");
        txtphone.setText("");
        txtaddress.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DeleteStudent().setVisible(true));
    }
}
