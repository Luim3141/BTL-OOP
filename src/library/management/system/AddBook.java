import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddBook extends JFrame {
    private JTextField txtid, txtname, txtauthor, txtpublisher, txtprice, txtyear;
    private JButton btnSave, btnClose;
    private JLabel lblBg;
    Connection c = Connect.ConnectToDB();
    PreparedStatement pst;

    public AddBook() {
        setTitle("Add Book");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(242, 242, 242);

        JLabel lblTitle = new JLabel("Add Book");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(red);
        lblTitle.setBounds(60, 40, 300, 40);
        add(lblTitle);

        // ==== LABELS ====
        JLabel lblID = new JLabel("Book ID");
        lblID.setFont(font18);
        lblID.setBounds(150, 150, 200, 40);
        add(lblID);

        JLabel lblName = new JLabel("Book Name");
        lblName.setFont(font18);
        lblName.setBounds(150, 230, 200, 40);
        add(lblName);

        JLabel lblAuthor = new JLabel("Author");
        lblAuthor.setFont(font18);
        lblAuthor.setBounds(150, 300, 200, 40);
        add(lblAuthor);

        JLabel lblPublisher = new JLabel("Publisher");
        lblPublisher.setFont(font18);
        lblPublisher.setBounds(150, 370, 200, 40);
        add(lblPublisher);

        JLabel lblPrice = new JLabel("Price");
        lblPrice.setFont(font18);
        lblPrice.setBounds(150, 440, 200, 40);
        add(lblPrice);

        JLabel lblYear = new JLabel("Year");
        lblYear.setFont(font18);
        lblYear.setBounds(150, 510, 200, 40);
        add(lblYear);

        // ==== TEXTFIELDS ====
        txtid = new JTextField();
        txtid.setFont(font18);
        txtid.setBounds(370, 150, 300, 40);
        add(txtid);

        txtname = new JTextField();
        txtname.setFont(font18);
        txtname.setBounds(370, 230, 300, 40);
        add(txtname);

        txtauthor = new JTextField();
        txtauthor.setFont(font18);
        txtauthor.setBounds(370, 300, 300, 40);
        add(txtauthor);

        txtpublisher = new JTextField();
        txtpublisher.setFont(font18);
        txtpublisher.setBounds(370, 370, 300, 40);
        add(txtpublisher);

        txtprice = new JTextField();
        txtprice.setFont(font18);
        txtprice.setBounds(370, 440, 300, 40);
        add(txtprice);

        txtyear = new JTextField();
        txtyear.setFont(font18);
        txtyear.setBounds(370, 510, 300, 40);
        add(txtyear);

        // ==== BUTTONS ====
        btnSave = new JButton("Save");
        btnSave.setFont(font18);
        btnSave.setBackground(red);
        btnSave.setForeground(white);
        btnSave.setBounds(350, 600, 120, 40);
        add(btnSave);

        btnClose = new JButton(new ImageIcon(getClass().getResource("/img/close icon.png")));
        btnClose.setBounds(1088, 0, 50, 40);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setContentAreaFilled(false);
        add(btnClose);

        // ==== BACKGROUND ====
        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnClose.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> saveBook());
    }

    private void saveBook() {
        if (txtid.getText().isEmpty() || txtname.getText().isEmpty() || txtauthor.getText().isEmpty() ||
                txtpublisher.getText().isEmpty() || txtprice.getText().isEmpty() || txtyear.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            pst = c.prepareStatement("INSERT INTO library.book (id, name, author, publisher, price, year, status) VALUES (?, ?, ?, ?, ?, ?, 'NotIssued')");
            pst.setString(1, txtid.getText());
            pst.setString(2, txtname.getText());
            pst.setString(3, txtauthor.getText());
            pst.setString(4, txtpublisher.getText());
            pst.setString(5, txtprice.getText());
            pst.setString(6, txtyear.getText());
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Book added successfully!");
            clearFields();
        } catch (SQLException ex) {
            Logger.getLogger(AddBook.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void clearFields() {
        txtid.setText("");
        txtname.setText("");
        txtauthor.setText("");
        txtpublisher.setText("");
        txtprice.setText("");
        txtyear.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AddBook().setVisible(true));
    }
}
