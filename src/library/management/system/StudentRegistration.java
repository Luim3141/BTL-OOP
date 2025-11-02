import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class StudentRegistration extends JFrame {
    private JTextField txtId, txtName, txtCourse, txtBranch, txtSemester, txtClass, txtEmail, txtPhone, txtAddress;
    private JComboBox<String> comboGender;
    private JButton btnSave, btnClose;
    private JLabel lblBg;
    Connection c = Connect.ConnectToDB();

    public StudentRegistration() {
        setTitle("Student Registration");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);

        JLabel lblTitle = new JLabel("Student Registration");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(white);
        lblTitle.setBounds(420, 60, 400, 40);
        add(lblTitle);

        JLabel[] labels = {
            new JLabel("Student ID:"), new JLabel("Student Name:"), new JLabel("Course:"),
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

        btnSave = new JButton("Save");
        btnSave.setFont(font18);
        btnSave.setBackground(red);
        btnSave.setForeground(white);
        btnSave.setBounds(380, 640, 150, 40);
        add(btnSave);

        btnClose = new JButton("Close");
        btnClose.setFont(font18);
        btnClose.setBackground(red);
        btnClose.setForeground(white);
        btnClose.setBounds(550, 640, 150, 40);
        add(btnClose);

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnSave.addActionListener(e -> saveStudent());
        btnClose.addActionListener(e -> dispose());
    }

    private void saveStudent() {
        try (PreparedStatement pst = c.prepareStatement(
                "INSERT INTO library.student (id, name, course, branch, semester, gender, class, email, phone, address) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            pst.setString(1, txtId.getText());
            pst.setString(2, txtName.getText());
            pst.setString(3, txtCourse.getText());
            pst.setString(4, txtBranch.getText());
            pst.setString(5, txtSemester.getText());
            pst.setString(6, comboGender.getSelectedItem().toString());
            pst.setString(7, txtClass.getText());
            pst.setString(8, txtEmail.getText());
            pst.setString(9, txtPhone.getText());
            pst.setString(10, txtAddress.getText());

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ Student added successfully!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "❌ Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentRegistration().setVisible(true));
    }
}
