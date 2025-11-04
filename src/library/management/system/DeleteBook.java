import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeleteBook extends JFrame {

    private JTextField txtid, txtname, txtauthor, txtpublisher, txtprice, txtyear;
    private JButton btnSearch, btnDelete, btnClose;
    private JLabel lblBg;

    Connection c = Connect.ConnectToDB();
    PreparedStatement pst;
    ResultSet rs;

    public DeleteBook() {
        setTitle("Delete Book");
        setSize(1140, 770);
        setLayout(null);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Font font18 = new Font("Segoe UI", Font.BOLD, 18);
        Color red = new Color(204, 0, 0);
        Color white = new Color(242, 242, 242);

        JLabel lblTitle = new JLabel("Delete Book");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(red);
        lblTitle.setBounds(60, 40, 300, 40);
        add(lblTitle);

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

        btnSearch = new JButton("Search");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSearch.setBackground(red);
        btnSearch.setForeground(white);
        btnSearch.setBounds(690, 150, 100, 40);
        add(btnSearch);

        btnDelete = new JButton("Delete");
        btnDelete.setFont(font18);
        btnDelete.setBackground(red);
        btnDelete.setForeground(white);
        btnDelete.setBounds(350, 600, 120, 40);
        add(btnDelete);

        btnClose = new JButton(new ImageIcon(getClass().getResource("/img/close icon.png")));
        btnClose.setBounds(1088, 0, 50, 40);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setContentAreaFilled(false);
        add(btnClose);

        lblBg = new JLabel(new ImageIcon(getClass().getResource("/img/All Page Backgraound.jpg")));
        lblBg.setBounds(0, 0, 1140, 770);
        add(lblBg);

        btnClose.addActionListener(e -> dispose());
        btnSearch.addActionListener(e -> searchBook());
        btnDelete.addActionListener(e -> deleteBook());
    }

    private void searchBook() {
        String id = txtid.getText();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Book ID first!");
            return;
        }

        try {
            pst = c.prepareStatement("SELECT * FROM library.book WHERE id=?");
            pst.setString(1, id);
            rs = pst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "No book found with this ID!");
                clearFields();
                return;
            }

            if ("Issued".equalsIgnoreCase(rs.getString("status"))) {
                JOptionPane.showMessageDialog(this, "This book is currently issued and cannot be deleted!");
                clearFields();
                return;
            }

            // Fill data if available and not issued
            txtname.setText(rs.getString("name"));
            txtauthor.setText(rs.getString("author"));
            txtpublisher.setText(rs.getString("publisher"));
            txtprice.setText(rs.getString("price"));
            txtyear.setText(rs.getString("year"));

        } catch (SQLException ex) {
            Logger.getLogger(DeleteBook.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void deleteBook() {
        String id = txtid.getText();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Book ID and search first!");
            return;
        }

        try {
            pst = c.prepareStatement("DELETE FROM library.book WHERE id=? AND status!='Issued'");
            pst.setString(1, id);
            int affected = pst.executeUpdate();

            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Book deleted successfully!");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "Cannot delete book! It may be currently issued or not found.");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DeleteBook.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void clearFields() {
        txtname.setText("");
        txtauthor.setText("");
        txtpublisher.setText("");
        txtprice.setText("");
        txtyear.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DeleteBook().setVisible(true));
    }
}
