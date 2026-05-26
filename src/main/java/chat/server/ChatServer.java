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
    private static final Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("[SERVER] Hệ thống APPCHAT đang khởi động");

        try {
            HibernateUtil.getSessionFactory();
            System.out.println("[SERVER - DATABASE] Kết nối MySQL qua Hibernate thành công!");
        } catch (Exception e) {
            System.err.println("[SERVER - CRITICAL LỖI] Không thể kết nối Database! Hãy kiểm tra lại MySQL.");
            e.printStackTrace();
            System.exit(1);
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Máy chủ đang lắng nghe kết nối trên cổng: " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] Có kết nối mới từ địa chỉ: " + socket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(socket, onlineUsers);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[SERVER LỖI] Sự cố luồng chính ServerSocket: " + e.getMessage());
        } finally {
            HibernateUtil.shutdown();
        }
    }
}