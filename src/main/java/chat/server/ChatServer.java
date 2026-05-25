package chat.server;

import chat.server.db.HibernateUtil;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main server that accepts TCP socket connections from chat clients.
 * Utilizes a multi‑threaded model to handle multiple concurrent clients.
 */
public class ChatServer {
    private static final int PORT = 12345;

    // Sử dụng ConcurrentHashMap để đảm bảo an toàn đa luồng (Thread-safe) khi quản lý danh sách Online
    private static final Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("[SERVER] Hệ thống Chat Tèo Coffee đang khởi động...");

        // Kích hoạt trước SessionFactory của Hibernate để kiểm tra kết nối Database MySQL
        try {
            HibernateUtil.getSessionFactory();
            System.out.println("[SERVER - DATABASE] Kết nối MySQL qua Hibernate thành công!");
        } catch (Exception e) {
            System.err.println("[SERVER - CRITICAL LỖI] Không thể kết nối Database! Hãy kiểm tra lại MySQL.");
            e.printStackTrace();
            System.exit(1);
        }

        // Mở cổng mạng Socket lắng nghe kết nối
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Máy chủ đang lắng nghe kết nối trên cổng: " + PORT);

            while (true) {
                // Chấp nhận kết nối từ một Client mới (Hàm này sẽ chặn/đợi cho đến khi có người vào)
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] Có kết nối mới từ địa chỉ: " + socket.getRemoteSocketAddress());

                // Tạo một bộ xử lý riêng biệt (ClientHandler) cho người dùng này
                ClientHandler handler = new ClientHandler(socket, onlineUsers);

                // Cấp phát một luồng độc lập (Thread) để chạy ngầm bộ xử lý đó, giải phóng Main Thread tiếp tục đón người khác
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[SERVER LỖI] Sự cố luồng chính ServerSocket: " + e.getMessage());
        } finally {
            // Đóng kết nối Hibernate an toàn khi tắt Server
            HibernateUtil.shutdown();
        }
    }
}