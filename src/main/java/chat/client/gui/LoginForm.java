package chat.client.gui;

import chat.client.network.ChatClientCore;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginForm extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContainer = new JPanel(cardLayout);

    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin;
    private JCheckBox showPassword;
    private final ChatClientCore core;

    // --- BẢNG MÀU UI PREMIUM ---
    private static final Color PRIMARY_BLUE = new Color(0, 132, 255);
    private static final Color HOVER_BLUE = new Color(0, 110, 220);
    private static final Color BG_GRADIENT_START = new Color(242, 246, 255);
    private static final Color BG_GRADIENT_END = new Color(218, 232, 255);
    private static final Color TEXT_MAIN = new Color(30, 35, 45);
    private static final Color TEXT_MUTED = new Color(110, 115, 125);

    public LoginForm() {
        core = new ChatClientCore();
        if (!core.connect()) {
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối đến Máy chủ Chat Tèo Coffee! Vui lòng kiểm tra lại Server.",
                    "Lỗi Mạng Socket", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        setTitle("Messenger - Đăng Nhập");
        setSize(460, 540); // Kích thước khung chuẩn hiện đại
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        mainContainer.add(createLoginPanel(), "LoginCard");
        mainContainer.add(new Register(this, core), "RegisterCard");
        add(mainContainer);
    }

    private JPanel createLoginPanel() {
        // Nền chính sử dụng Gradient mượt mà
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

        // Khung nội dung trắng (Card) - Đổ bóng nhẹ & Bo tròn 24px
        JPanel card = new JPanel(new GridBagLayout()) { // Sửa thành GridBagLayout để chống lệch
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Hiệu ứng đổ bóng mờ giả lập (Elevation)
                g2d.setColor(new Color(0, 0, 0, 10));
                for (int i = 1; i <= 5; i++) {
                    g2d.fillRoundRect(i, i, getWidth() - (i * 2), getHeight() - (i * 2), 24, 24);
                }

                // Nền trắng chính
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 24, 24);
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(360, 440));
        card.setBorder(new EmptyBorder(30, 25, 30, 25));

        // Cấu hình GridBagConstraints để ép các thành phần xếp dọc thẳng hàng 100%
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 4, 0); // Khoảng cách dưới

        // ---- 1. TIÊU ĐỀ CHÍNH ----
        JLabel lblTitle = new JLabel("Chào mừng trở lại", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(TEXT_MAIN);
        card.add(lblTitle, gbc);

        // ---- 2. TIÊU ĐỀ PHỤ ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 24, 0);
        JLabel lblSub = new JLabel("Đăng nhập để kết nối hệ thống trò chuyện", JLabel.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_MUTED);
        card.add(lblSub, gbc);

        // Định dạng font chung cho form
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
        gbc.insets = new Insets(0, 0, 16, 0);
        txtUser = new JTextField();
        txtUser.setFont(inputFont);
        txtUser.putClientProperty("JTextField.placeholderText", "Nhập tên tài khoản...");
        txtUser.putClientProperty("JComponent.roundRect", true); // Bo tròn chuẩn FlatLaf
        txtUser.setPreferredSize(new Dimension(310, 38));
        card.add(txtUser, gbc);

        // ---- 5. NHÃN MẬT KHẨU ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 4, 4, 4);
        JLabel lblPass = new JLabel("MẬT KHẨU");
        lblPass.setFont(labelFont);
        lblPass.setForeground(TEXT_MUTED);
        card.add(lblPass, gbc);

        // ---- 6. Ô NHẬP MẬT KHẨU ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        txtPass = new JPasswordField();
        txtPass.setFont(inputFont);
        txtPass.putClientProperty("JTextField.placeholderText", "Nhập mật khẩu...");
        txtPass.putClientProperty("JComponent.roundRect", true); // Bo tròn chuẩn FlatLaf
        txtPass.setPreferredSize(new Dimension(310, 38));
        card.add(txtPass, gbc);

        // ---- 7. CHECKBOX HIỂN THỊ MẬT KHẨU ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 24, 0);
        showPassword = new JCheckBox("Hiển thị mật khẩu");
        showPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPassword.setForeground(TEXT_MUTED);
        showPassword.setOpaque(false);
        showPassword.setFocusPainted(false);
        showPassword.addActionListener(e -> {
            if (showPassword.isSelected()) {
                txtPass.setEchoChar((char) 0);
            } else {
                txtPass.setEchoChar('*');
            }
        });
        card.add(showPassword, gbc);

        // ---- 8. NÚT ĐĂNG NHẬP ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 20, 0);
        btnLogin = new JButton("Đăng Nhập") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); // Nút phẳng bo tròn góc mượt
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(PRIMARY_BLUE);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setPreferredSize(new Dimension(310, 42));
        btnLogin.setFocusPainted(false);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hiệu ứng di chuột đổi màu nút mượt mà
        btnLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btnLogin.isEnabled()) btnLogin.setBackground(HOVER_BLUE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (btnLogin.isEnabled()) btnLogin.setBackground(PRIMARY_BLUE);
            }
        });
        btnLogin.addActionListener(e -> checkLogin(txtUser.getText().trim(), String.valueOf(txtPass.getPassword())));
        card.add(btnLogin, gbc);

        // ---- 9. ĐƯỜNG LINK ĐĂNG KÝ ----
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel lblRegister = new JLabel("<html>Chưa có tài khoản? <span style='color:rgb(0,132,255); font-weight:bold; text-decoration:underline;'>Đăng ký ngay</span></html>", JLabel.CENTER);
        lblRegister.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblRegister.setForeground(TEXT_MAIN);
        lblRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblRegister.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCard("RegisterCard");
            }
        });
        card.add(lblRegister, gbc);

        // Đóng gói card vào panel chính
        mainPanel.add(card);
        return mainPanel;
    }

    public void showCard(String name) {
        cardLayout.show(mainContainer, name);
    }

    public void checkLogin(String name, String pass) {
        if (name.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ tài khoản và mật khẩu!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setBackground(TEXT_MUTED);

        String response = core.executeAuthAction("LOGIN", name, pass);

        if (response.startsWith("SUCCESS")) {
            JOptionPane.showMessageDialog(this, "Đăng nhập thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);

            ChatClientUI chatUI = new ChatClientUI(name, core);
            chatUI.setVisible(true);

            core.startListening();
            this.dispose();
        } else {
            String errorReason = response.substring(response.indexOf(":") + 1);
            JOptionPane.showMessageDialog(this, errorReason, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
            btnLogin.setEnabled(true);
            btnLogin.setBackground(PRIMARY_BLUE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
            } catch (Exception e) {
                e.printStackTrace();
            }
            new LoginForm().setVisible(true);
        });
    }
}