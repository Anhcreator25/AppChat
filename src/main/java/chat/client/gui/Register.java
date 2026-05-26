package chat.client.gui; // PHẢI TRÙNG PACKAGE VỚI LOGINFORM

import chat.client.network.ChatClientCore; // Để nhận diện đối tượng core truyền sang
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Register extends JPanel {

    private JTextField txtUser;
    private JPasswordField txtPassword;
    private JPasswordField confirmpass;
    private JCheckBox showPassword;
    private JButton creatAcc;

    private final LoginForm loginForm;
    private final ChatClientCore core; // Sử dụng chung lõi mạng kết nối từ LoginForm chuyển sang

    // --- BẢNG MÀU UI PREMIUM ĐỒNG BỘ VỚI LOGINFORM ---
    private static final Color PRIMARY_BLUE = new Color(0, 132, 255);
    private static final Color HOVER_BLUE = new Color(0, 110, 220);
    private static final Color BG_GRADIENT_START = new Color(242, 246, 255);
    private static final Color BG_GRADIENT_END = new Color(218, 232, 255);
    private static final Color TEXT_MAIN = new Color(30, 35, 45);
    private static final Color TEXT_MUTED = new Color(110, 115, 125);

    public Register(LoginForm loginForm, ChatClientCore core) {
        this.loginForm = loginForm;
        this.core = core;

        // Giữ nguyên kích thước ban đầu để vừa vặn trong CardLayout của LoginForm
        setSize(460, 540);
        setLayout(new BorderLayout());

        // Nền chính sử dụng Gradient mượt mà đồng bộ tuyệt đối
        JPanel mainPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, BG_GRADIENT_START, 0, getHeight(), BG_GRADIENT_END);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        add(mainPanel, BorderLayout.CENTER);

        // Khung nội dung trắng (Card) - Đổ bóng nhẹ & Bo tròn 24px (GridBagLayout chống lệch)
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Hiệu ứng đổ bóng mờ giả lập chiều sâu
                g2d.setColor(new Color(0, 0, 0, 10));
                for (int i = 1; i <= 5; i++) {
                    g2d.fillRoundRect(i, i, getWidth() - (i * 2), getHeight() - (i * 2), 24, 24);
                }

                // Nền trắng chính của Card
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 24, 24);
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(360, 470)); // Chiều cao tăng nhẹ để đủ chỗ cho 3 ô nhập liệu
        card.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Cấu hình GridBagConstraints để ép toàn bộ thành phần xếp dọc thẳng hàng 100%
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 4, 0);

        // ---- 1. TIÊU ĐỀ CHÍNH MÀN HÌNH ĐĂNG KÝ ----
        JLabel lblTitle = new JLabel("Đăng ký tài khoản", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(TEXT_MAIN);
        card.add(lblTitle, gbc);

        // ---- 2. TIÊU ĐỀ PHỤ ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 20, 0);
        JLabel lblSub = new JLabel("Tạo tài khoản miễn phí để tham gia phòng chat", JLabel.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_MUTED);
        card.add(lblSub, gbc);

        // Định dạng font chung cho form giống hệt LoginForm
        Font labelFont = new Font("Segoe UI", Font.BOLD, 12);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 14);

        // ---- 3. NHÃN TÀI KHOẢN ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 4, 4, 4);
        JLabel lblUser = new JLabel("TÀI KHOẢN");
        lblUser.setFont(labelFont);
        lblUser.setForeground(TEXT_MUTED);
        card.add(lblUser, gbc);

        // ---- 4. Ô NHẬP TÀI KHOẢN ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 14, 0);
        txtUser = new JTextField();
        txtUser.setFont(inputFont);
        txtUser.putClientProperty("JTextField.placeholderText", "Nhập tên tài khoản mong muốn...");
        txtUser.putClientProperty("JComponent.roundRect", true); // Ép bo tròn thanh nhập của FlatLaf
        txtUser.setPreferredSize(new Dimension(310, 36));
        card.add(txtUser, gbc);

        // ---- 5. NHÃN MẬT KHẨU ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 4, 4, 4);
        JLabel lblPasswordLabel = new JLabel("MẬT KHẨU");
        lblPasswordLabel.setFont(labelFont);
        lblPasswordLabel.setForeground(TEXT_MUTED);
        card.add(lblPasswordLabel, gbc);

        // ---- 6. Ô NHẬP MẬT KHẨU ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 14, 0);
        txtPassword = new JPasswordField();
        txtPassword.setFont(inputFont);
        txtPassword.putClientProperty("JTextField.placeholderText", "Nhập mật khẩu ...");
        txtPassword.putClientProperty("JComponent.roundRect", true);
        txtPassword.setPreferredSize(new Dimension(310, 36));
        card.add(txtPassword, gbc);

        // ---- 7. NHÃN XÁC NHẬN MẬT KHẨU ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 4, 4, 4);
        JLabel lblConfirm = new JLabel("XÁC NHẬN MẬT KHẨU");
        lblConfirm.setFont(labelFont);
        lblConfirm.setForeground(TEXT_MUTED);
        card.add(lblConfirm, gbc);

        // ---- 8. Ô NHẬP XÁC NHẬN MẬT KHẨU ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 6, 0);
        confirmpass = new JPasswordField();
        confirmpass.setFont(inputFont);
        confirmpass.putClientProperty("JTextField.placeholderText",null);
        confirmpass.putClientProperty("JComponent.roundRect", true);
        confirmpass.setPreferredSize(new Dimension(310, 36));
        card.add(confirmpass, gbc);

        // ---- 9. CHECKBOX HIỂN THỊ MẬT KHẨU ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 18, 0);
        showPassword = new JCheckBox("Hiển thị mật khẩu");
        showPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPassword.setForeground(TEXT_MUTED);
        showPassword.setOpaque(false);
        showPassword.setFocusPainted(false);
        showPassword.addActionListener(e -> {
            if (showPassword.isSelected()) {
                txtPassword.setEchoChar((char) 0);
                confirmpass.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('*');
                confirmpass.setEchoChar('*');
            }
        });
        card.add(showPassword, gbc);

        // ---- 10. NÚT ĐĂNG KÝ / TẠO TÀI KHOẢN (ĐỔI SANG MÀU XANH HIỆN ĐẠI) ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 16, 0);
        creatAcc = new JButton("Tạo Tài Khoản") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); // Bo tròn nút phẳng hiện đại
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        creatAcc.setFont(new Font("Segoe UI", Font.BOLD, 14));
        creatAcc.setBackground(PRIMARY_BLUE); // Đổi từ màu nâu gỗ cũ sang màu xanh Messenger đồng bộ
        creatAcc.setForeground(Color.WHITE);
        creatAcc.setPreferredSize(new Dimension(310, 42));
        creatAcc.setFocusPainted(false);
        creatAcc.setContentAreaFilled(false);
        creatAcc.setBorderPainted(false);
        creatAcc.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hiệu ứng di chuột đổi màu nút mượt mà khi rê chuột vào nút tạo tài khoản
        creatAcc.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (creatAcc.isEnabled()) creatAcc.setBackground(HOVER_BLUE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (creatAcc.isEnabled()) creatAcc.setBackground(PRIMARY_BLUE);
            }
        });
        creatAcc.addActionListener(e -> checkinfor());
        card.add(creatAcc, gbc);

        // ---- 11. ĐƯỜNG LINK QUAY LẠI MÀN HÌNH ĐĂNG NHẬP ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel lblBackToLogin = new JLabel("<html>Đã có tài khoản? <span style='color:rgb(0,132,255); font-weight:bold; text-decoration:underline;'>Đăng nhập ngay</span></html>", JLabel.CENTER);
        lblBackToLogin.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblBackToLogin.setForeground(TEXT_MAIN);
        lblBackToLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblBackToLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Quay lại card đăng nhập
                loginForm.showCard("LoginCard");
            }
        });
        card.add(lblBackToLogin, gbc);

        // Đóng gói card vào panel chính
        mainPanel.add(card);
    }

    /**
     * SỬA ĐỔI: Gửi gói tin REGISTER qua Socket mạng thay vì INSERT INTO JDBC
     */
    public void checkinfor() {
        String name = txtUser.getText().trim();
        String pass = String.valueOf(txtPassword.getPassword());
        String confirm = String.valueOf(confirmpass.getPassword());

        if (name.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            txtPassword.setText("");
            confirmpass.setText("");
            return;
        }

        creatAcc.setEnabled(false); // Khóa nút tạm thời
        creatAcc.setBackground(TEXT_MUTED);

        // Đẩy thông tin đăng ký lên TCP Server qua đối tượng lõi core
        String response = core.executeAuthAction("REGISTER", name, pass);

        if (response.startsWith("SUCCESS")) {
            String successMsg = response.substring(response.indexOf(":") + 1);
            JOptionPane.showMessageDialog(this, successMsg, "Thành công", JOptionPane.INFORMATION_MESSAGE);

            // Xóa sạch text form cũ để chuẩn bị cho lần sau
            txtUser.setText("");
            txtPassword.setText("");
            confirmpass.setText("");

            // Chuyển card layout quay về thẻ đăng nhập
            loginForm.showCard("LoginCard");
        } else {
            String errorMsg = response.substring(response.indexOf(":") + 1);
            JOptionPane.showMessageDialog(this, errorMsg, "Đăng ký thất bại", JOptionPane.ERROR_MESSAGE);
        }

        creatAcc.setEnabled(true);
        creatAcc.setBackground(PRIMARY_BLUE);
    }
}