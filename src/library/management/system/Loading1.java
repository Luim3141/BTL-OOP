import javax.swing.*;

public class Loading1 extends JFrame {

    private JLabel backgroundLabel;

    public Loading1() {
        // --- Cấu hình cơ bản ---
        setTitle("Loading Screen");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setLayout(null);
        setSize(1370, 770);
        setLocationRelativeTo(null);

        // --- Thêm hình nền ---
        backgroundLabel = new JLabel();
        backgroundLabel.setIcon(new ImageIcon(getClass().getResource("/img/x.jpg")));
        backgroundLabel.setBounds(0, 0, 1370, 770);
        add(backgroundLabel);

        // --- Hiển thị form ---
        setVisible(true);

        // --- Chuyển sang Loading2 sau 1 giây ---
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(1000);
                new Loading2().setVisible(true);
                dispose(); // đóng Loading1
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });
        t.start();
    }

    public static void main(String[] args) {
        // --- Nimbus Look and Feel ---
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(Loading1::new);
    }
}
  