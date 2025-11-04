import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class IssueAdminRQ extends JFrame {
    private JButton btnViewList, btnInspect, btnClose;

    public IssueAdminRQ() {
        setTitle("Issue Requests (Admin)");
        setSize(600, 400);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = Color.WHITE;

        btnViewList = new JButton("View Request List");
        btnViewList.setBounds(180, 100, 220, 50);
        btnViewList.setBackground(red);
        btnViewList.setForeground(white);
        btnViewList.setFont(font18);
        add(btnViewList);

        btnInspect = new JButton("Inspect Request");
        btnInspect.setBounds(180, 180, 220, 50);
        btnInspect.setBackground(red);
        btnInspect.setForeground(white);
        btnInspect.setFont(font18);
        add(btnInspect);

        btnClose = new JButton("Close");
        btnClose.setBounds(240, 270, 100, 40);
        add(btnClose);

        btnViewList.addActionListener(e -> new RequestList().setVisible(true));
        btnInspect.addActionListener(e -> new InspectRequest().setVisible(true));
        btnClose.addActionListener(e -> dispose());
    }

    public static void main(String[] args) {
        new IssueAdminRQ().setVisible(true);
    }
}
