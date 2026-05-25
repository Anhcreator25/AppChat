package chat.client.gui;

import chat.client.network.ChatClientCore;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
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

    public LoginForm() {
        core = new ChatClientCore();
        if (!core.connect()) {
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối đến Máy chủ Chat Tèo Coffee! Vui lòng kiểm tra lại Server.",
                    "Lỗi Mạng Socket", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        setTitle("APP CHAT");
        setSize(450, 380);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        mainContainer.add(createLoginPanel(), "LoginCard");
        mainContainer.add(new Register(this, core), "RegisterCard");
        add(mainContainer);
    }

    private JPanel createLoginPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(0, 145, 255));

        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(320, 280));
        card.setBackground(Color.WHITE);
        card.setLayout(null);
        card.setBorder(BorderFactory.createLineBorder(new Color(0, 145, 255)));
        mainPanel.add(card);

        JLabel lblTitle = new JLabel("APP CHAT", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setBounds(60, 10, 200, 30);
        card.add(lblTitle);

        JLabel lbl_title = new JLabel("Hệ thống trò chuyện ", JLabel.CENTER);
        lbl_title.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl_title.setForeground(Color.GRAY);
        lbl_title.setBounds(50, 40, 220, 20);
        card.add(lbl_title);

        Font font = new Font("Segoe UI", Font.PLAIN, 13);

        JLabel lblUser = new JLabel("Tài khoản");
        lblUser.setFont(font);
        lblUser.setBounds(30, 70, 100, 20);
        card.add(lblUser);

        txtUser = new JTextField();
        txtUser.setFont(font);
        txtUser.setBounds(30, 90, 260, 30);
        card.add(txtUser);

        JLabel lblPass = new JLabel("Mật khẩu");
        lblPass.setFont(font);
        lblPass.setBounds(30, 120, 100, 20);
        card.add(lblPass);

        txtPass = new JPasswordField();
        txtPass.setFont(font);
        txtPass.setBounds(30, 140, 260, 30);
        card.add(txtPass);

        showPassword = new JCheckBox("Hiện mật khẩu");
        showPassword.setFont(font);
        showPassword.setBackground(Color.WHITE);
        showPassword.setBounds(30, 175, 260, 20);
        card.add(showPassword);

        showPassword.addActionListener(e -> {
            if (showPassword.isSelected()) {
                txtPass.setEchoChar((char) 0);
            } else {
                txtPass.setEchoChar('*');
            }
        });

        btnLogin = new JButton("ĐĂNG NHẬP");
        btnLogin.setBounds(30, 201, 260, 32);
        btnLogin.setBackground(new Color(0, 145, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setFocusPainted(false);
        card.add(btnLogin);

        btnLogin.addActionListener(e -> checkLogin(txtUser.getText().trim(), String.valueOf(txtPass.getPassword())));

        JLabel lblregister = new JLabel("<html>Bạn chưa có tài khoản? <u>Đăng Ký</u></html>", JLabel.CENTER);
        lblregister.setBounds(60, 230, 200, 25);
        lblregister.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblregister.setForeground(new Color(0, 145, 255));
        lblregister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.add(lblregister);

        lblregister.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCard("RegisterCard");
            }
        });

        return mainPanel;
    }

    public void showCard(String name) {
        cardLayout.show(mainContainer, name);
    }

    public void checkLogin(String name, String pass) {
        if (name.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        btnLogin.setEnabled(false);

        String response = core.executeAuthAction("LOGIN", name, pass);

        if (response.startsWith("SUCCESS")) {
            JOptionPane.showMessageDialog(this, "Đăng nhập thành công!");

            // Mở màn hình chat chính thức (đăng ký các listener trước)
            ChatClientUI chatUI = new ChatClientUI(name, core);
            chatUI.setVisible(true);

            // KÍCH HOẠT MẠNG LẮNG NGHE NGẦM SAU KHI UI ĐÃ ĐĂNG KÝ LISTENER
            core.startListening();

            // Đóng cửa sổ đăng nhập
            this.dispose();
        } else {
            String errorReason = response.substring(response.indexOf(":") + 1);
            JOptionPane.showMessageDialog(this, errorReason, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
            btnLogin.setEnabled(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
            } catch (Exception e) { e.printStackTrace(); }
            new LoginForm().setVisible(true);
        });
    }
}