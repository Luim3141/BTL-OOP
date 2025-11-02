import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AdjustStudentInfo extends JFrame {
    private JTextField txtId, txtName, txtCourse, txtBranch, txtSemester, txtClass, txtEmail, txtPhone, txtAddress;
    private JComboBox<String> comboGender;
    private JButton btnSearch, btnUpdate, btnClose;
    private JLabel lblBg;
    Connection c = Connect.ConnectToDB();
    private boolean canEdit = false;

    public AdjustStudentInfo() {
        setTitle("Adjust Student Info");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);

        JLabel lblTitle = new JLabel("Adjust Student Information");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(white);
        lblTitle.setBounds(360, 60, 400, 40);
        add(lblTitle);

        JLabel[] labels = {
            new JLabel("Student ID:"), new JLabel("Name:"), new JLabel("Course:"),
            new JLabel("Branch:"), new JLabel("Semester:"), new JLabel("Gender:"),
            new JLabel("Class:"), new JLabel("Email:"), new JLabel("Phone:"), new JLabel("Address:")
        };

        int y = 140;
        for (JLabel lbl : labels) {
            lbl.setFont(font18);
            lbl.setForeground(white);
            lbl.setBounds(220, y, 160, 40);
            add(lbl);
            y += 50;
        }

        txtId = new JTextField();
        txtName = new JTextField();
        txtCourse = new JTextField();
        txtBranch = new JTextField();
        txtSemester = new JTextField();
        comboGender = new JComboBox<>(new String[]{"Male", "Female"});
        txtClass = new JTextField();
        txtEmail = new JTextField();
        txtPhone = new JTextField();
        txtAddress = new JTextField();

        JComponent[] fields = {
            txtId, txtName, txtCourse, txtBranch, txtSemester, comboGender,
            txtClass, txtEmail, txtPhone, txtAddress
        };

        y = 140;
        for (JComponent f : fields) {
            f.setFont(font18);
            f.setBounds(380, y, 400, 40);
            add(f);
            y += 50;
        }

        btnSearch = new JButton("Search");
        btnSearch.setFont(font18);
        btnSearch.setBackground(red);
        btnSearch.setForeground(white);
        btnSearch.setBounds(800, 140, 120, 40);
        add(btnSearch);

        btnUpdate = new JButton("Update");
        btnUpdate.setFont(font18);
        btnUpdate.setBackground(red);
        btnUpdate.setForeground(white);
        btnUpdate.setBounds(400, 680, 150, 40);
        add(btnUpdate);

        btnClose = new JButton("Close");
        btnClose.setFont(font18);
        btnClose.setBackground(red);
        btnClose.setForeground(white);
        btnClose.setBounds(580, 680, 150, 40);
        add(btnClose);

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnSearch.addActionListener(e -> searchStudent());
        btnUpdate.addActionListener(e -> updateStudent());
        btnClose.addActionListener(e -> dispose());
    }

    private void searchStudent() {
        canEdit = false;
        try (PreparedStatement pst = c.prepareStatement("SELECT * FROM library.student WHERE id=?")) {
            pst.setString(1, txtId.getText());
            ResultSet rs = pst.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "❌ No student found with ID: " + txtId.getText());
                return;
            }

            txtName.setText(rs.getString("name"));
            txtCourse.setText(rs.getString("course"));
            txtBranch.setText(rs.getString("branch"));
            txtSemester.setText(rs.getString("semester"));
            comboGender.setSelectedItem(rs.getString("gender"));
            txtClass.setText(rs.getString("class"));
            txtEmail.setText(rs.getString("email"));
            txtPhone.setText(rs.getString("phone"));
            txtAddress.setText(rs.getString("address"));
            canEdit = true;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void updateStudent() {
        if (!canEdit) {
            JOptionPane.showMessageDialog(this, "⚠️ Search student before updating!");
            return;
        }

        try (PreparedStatement pst = c.prepareStatement(
                "UPDATE library.student SET name=?, course=?, branch=?, semester=?, gender=?, class=?, email=?, phone=?, address=? WHERE id=?")) {
            pst.setString(1, txtName.getText());
            pst.setString(2, txtCourse.getText());
            pst.setString(3, txtBranch.getText());
            pst.setString(4, txtSemester.getText());
            pst.setString(5, comboGender.getSelectedItem().toString());
            pst.setString(6, txtClass.getText());
            pst.setString(7, txtEmail.getText());
            pst.setString(8, txtPhone.getText());
            pst.setString(9, txtAddress.getText());
            pst.setString(10, txtId.getText());
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Student updated successfully!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "❌ Update failed: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdjustStudentInfo().setVisible(true));
    }
}
