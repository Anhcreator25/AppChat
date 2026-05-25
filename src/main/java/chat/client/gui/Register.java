package chat.client.gui; // PHẢI TRÙNG PACKAGE VỚI LOGINFORM

import chat.client.network.ChatClientCore; // Để nhận diện đối tượng core truyền sang
import javax.swing.*;
import java.awt.*;
public class Register extends JPanel {

    private JTextField txtUser;
    private JPasswordField txtPassword;
    private JPasswordField confirmpass;
    private JCheckBox showPassword;
    private JButton creatAcc;

    private final LoginForm loginForm;
    private final ChatClientCore core; // Sử dụng chung lõi mạng kết nối từ LoginForm chuyển sang

    public Register(LoginForm loginForm, ChatClientCore core) {
        this.loginForm = loginForm;
        this.core = core;

        setSize(440, 350);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(245, 239, 230));
        add(mainPanel, BorderLayout.CENTER);

        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(340, 310));
        card.setBackground(Color.WHITE);
        card.setLayout(null);
        card.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        mainPanel.add(card);

        JLabel lblTitle = new JLabel(" ĐĂNG KÝ TÀI KHOẢN", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setBounds(40, 15, 260, 30);
        card.add(lblTitle);

        Font font = new Font("Segoe UI", Font.PLAIN, 13);

        JLabel lblUser = new JLabel("Tài khoản");
        lblUser.setFont(font);
        lblUser.setBounds(30, 60, 100, 20);
        card.add(lblUser);

        txtUser = new JTextField();
        txtUser.setFont(font);
        txtUser.setBounds(30, 80, 280, 30);
        card.add(txtUser);

        JLabel lblPassword = new JLabel("Mật khẩu");
        lblPassword.setFont(font);
        lblPassword.setBounds(30, 115, 100, 20);
        card.add(lblPassword);

        txtPassword = new JPasswordField();
        txtPassword.setFont(font);
        txtPassword.setBounds(30, 135, 280, 30);
        card.add(txtPassword);

        JLabel lblConfirm = new JLabel("Xác nhận mật khẩu");
        lblConfirm.setFont(font);
        lblConfirm.setBounds(30, 170, 120, 20);
        card.add(lblConfirm);

        confirmpass = new JPasswordField();
        confirmpass.setFont(font);
        confirmpass.setBounds(30, 190, 280, 30);
        card.add(confirmpass);

        showPassword = new JCheckBox("Hiện mật khẩu");
        showPassword.setFont(font);
        showPassword.setBackground(Color.WHITE);
        showPassword.setBounds(30, 215, 260, 26);
        card.add(showPassword);

        showPassword.addActionListener(e -> {
            if (showPassword.isSelected()) {
                txtPassword.setEchoChar((char) 0);
                confirmpass.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('*');
                confirmpass.setEchoChar('*');
            }
        });

        creatAcc = new JButton("TẠO TÀI KHOẢN");
        creatAcc.setBounds(30, 243, 280, 33);
        creatAcc.setBackground(new Color(111, 78, 55));
        creatAcc.setForeground(Color.WHITE);
        creatAcc.setFont(new Font("Segoe UI", Font.BOLD, 14));
        creatAcc.setFocusPainted(false);
        card.add(creatAcc);

        creatAcc.addActionListener(e -> checkinfor());
    }

    /**
     * SỬA ĐỔI: Gửi gói tin REGISTER qua Socket mạng thay vì INSERT INTO JDBC
     */
    public void checkinfor() {
        String name = txtUser.getText().trim();
        String pass = String.valueOf(txtPassword.getPassword());
        String confirm = String.valueOf(confirmpass.getPassword());

        if (name.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin");
            return;
        }

        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu không khớp");
            txtPassword.setText("");
            confirmpass.setText("");
            return;
        }

        creatAcc.setEnabled(false); // Khóa nút tạm thời

        // Đẩy thông tin đăng ký lên TCP Server qua đối tượng lõi core
        String response = core.executeAuthAction("REGISTER", name, pass);

        if (response.startsWith("SUCCESS")) {
            String successMsg = response.substring(response.indexOf(":") + 1);
            JOptionPane.showMessageDialog(this, successMsg);

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
    }
}