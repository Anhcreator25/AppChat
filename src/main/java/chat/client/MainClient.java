package chat.client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the chat client application.
 */
public class MainClient {
    public static void main(String[] args) {
        // Chạy giao diện an toàn trên luồng xử lý sự kiện của Swing (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Nạp giao diện FlatLaf hiện đại cho hệ thống
                com.formdev.flatlaf.FlatLightLaf.setup();

                // 2. Cấu hình bo góc mềm mại cho nút bấm và ô nhập giống Zalo (Tùy chọn)
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);

            } catch (Exception e) {
                System.err.println("[MAIN CLIENT] Không thể nạp FlatLaf, hệ thống sẽ dùng giao diện mặc định.");
            }

            // 3. Khởi tạo và hiển thị cửa sổ Đăng nhập
            chat.client.gui.LoginForm loginForm = new chat.client.gui.LoginForm();
            loginForm.setVisible(true); // <--- BẮT BUỘC PHẢI CÓ DÒNG NÀY ĐỂ HIỂN THỊ CỬA SỔ
        });
    }
}