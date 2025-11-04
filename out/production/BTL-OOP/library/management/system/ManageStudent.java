import javax.swing.*;
import java.awt.*;

public class ManageStudent extends JFrame {
    private JButton btnStudentList, btnDeleteStudent, btnAdjustStudent, btnAddStudent, btnClose;
    private JLabel lblBg;

    public ManageStudent() {
        setTitle("Manage Student");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(255, 255, 255);

        JLabel lblTitle = new JLabel("Manage Students");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(white);
        lblTitle.setBounds(420, 70, 300, 40);
        add(lblTitle);

        // Các nút chức năng
        btnStudentList = new JButton("Student List");
        btnDeleteStudent = new JButton("Delete Student");
        btnAdjustStudent = new JButton("Adjust Student Info");
        btnAddStudent = new JButton("Add Student");
        btnClose = new JButton("Close");

        JButton[] buttons = {btnStudentList, btnDeleteStudent, btnAdjustStudent, btnAddStudent, btnClose};
        int y = 200;
        for (JButton b : buttons) {
            b.setFont(font18);
            b.setBackground(red);
            b.setForeground(white);
            b.setBounds(420, y, 300, 60);
            add(b);
            y += 80;
        }

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        // === Các sự kiện ===
        btnStudentList.addActionListener(e -> new StudentList().setVisible(true));
        btnDeleteStudent.addActionListener(e -> new DeleteStudent().setVisible(true));
        btnAdjustStudent.addActionListener(e -> new AdjustStudentInfo().setVisible(true));
        btnAddStudent.addActionListener(e -> new StudentRegistration().setVisible(true));
        btnClose.addActionListener(e -> dispose());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ManageStudent().setVisible(true));
    }
}
