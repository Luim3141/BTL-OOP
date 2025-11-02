import javax.swing.*;
import java.awt.*;

public class Loading2 extends JFrame {

    private JPanel panel;
    private JLabel imageLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    public Loading2() {
        // --- Cấu hình cơ bản ---
        setTitle("Loading Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setSize(650, 460);
        setLocationRelativeTo(null);
        setLayout(null);

        // --- Panel chính ---
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(new Color(0, 255, 0)); // xanh lá
        panel.setBounds(0, 0, 650, 460);
        add(panel);

        // --- Ảnh nền ---
        imageLabel = new JLabel();
        imageLabel.setIcon(new ImageIcon(getClass().getResource("/img/Picsart_23-10-30_17-47-04-022.jpg")));
        imageLabel.setBounds(10, 10, 630, 400);
        panel.add(imageLabel);

        // --- Thanh tiến trình ---
        progressBar = new JProgressBar();
        progressBar.setBounds(0, 440, 650, 16);
        progressBar.setBackground(new Color(0, 0, 153));
        progressBar.setForeground(new Color(0, 0, 153));
        progressBar.setStringPainted(true);
        panel.add(progressBar);

        // --- Nhãn hiển thị trạng thái ---
        statusLabel = new JLabel("");
        statusLabel.setBounds(400, 420, 247, 22);
        panel.add(statusLabel);

        setVisible(true);

        // --- Luồng chạy tiến trình ---
        Thread t = new Thread(() -> {
            for (int i = 0; i <= 100; i++) {
                try {
                    progressBar.setValue(i);
                    Thread.sleep(50);

                    if (i == 25) {
                        statusLabel.setText("Connecting Database...");
                    } else if (i == 50) {
                        statusLabel.setText("Loading Modules...");
                    } else if (i == 95) {
                        statusLabel.setText("Launching Application...");
                    } else if (i == 100) {
                        new SignIn().setVisible(true); // mở form tiếp theo
                        dispose();
                    }

                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
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

        SwingUtilities.invokeLater(Loading2::new);
    }
}
