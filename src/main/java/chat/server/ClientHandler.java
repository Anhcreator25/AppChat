package chat.server;

import chat.shared.model.User;
import chat.shared.model.ChatMessage;
import chat.shared.util.PasswordHasher;
import chat.server.db.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Map<String, ClientHandler> onlineUsers;
    private BufferedReader in;
    private PrintWriter out;
    private String username = null;

    public ClientHandler(Socket socket, Map<String, ClientHandler> onlineUsers) {
        this.socket = socket;
        this.onlineUsers = onlineUsers;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String requestLine;
            while ((requestLine = in.readLine()) != null) {
                String[] tokens = requestLine.split("\\|", 3);
                if (tokens.length < 3) continue;

                String command = tokens[0];

                if ("REGISTER".equals(command)) {
                    handleRegister(tokens[1], tokens[2]);
                } else if ("LOGIN".equals(command)) {
                    if (handleLogin(tokens[1], tokens[2])) {
                        this.username = tokens[1];
                        onlineUsers.put(username, this);
                        System.out.println("[SERVER] Người dùng @" + username + " đã ONLINE.");
                        broadcastUserList();
                        break;
                    }
                }
            }

            if (this.username != null) {
                while ((requestLine = in.readLine()) != null) {
                    String[] chatTokens = requestLine.split("\\|", 4);
                    if (chatTokens.length >= 4 && "CHAT".equals(chatTokens[0])) {
                        String toUser = chatTokens[1];
                        String type = chatTokens[2];
                        String payload = chatTokens[3];

                        handleChatMessage(toUser, type, payload);
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("[SERVER] Người dùng " + (username != null ? username : "Chưa đăng nhập") + " mất kết nối.");
        } finally {
            closeConnection();
        }
    }

    private void handleRegister(String user, String pass) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            User existing = session.createQuery("from User where username = :u", User.class)
                    .setParameter("u", user).uniqueResult();

            if (existing != null) {
                out.println("REGISTER_RESPONSE|FAILED|Tên tài khoản này đã tồn tại trên hệ thống!");
            } else {
                String hashedPassword = PasswordHasher.hashPassword(pass);
                User newUser = new User(user, hashedPassword);

                session.save(newUser);
                tx.commit();
                out.println("REGISTER_RESPONSE|SUCCESS|Đăng ký tài khoản thành công!");
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            out.println("REGISTER_RESPONSE|FAILED|Lỗi cơ sở dữ liệu hệ thống!");
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    private boolean handleLogin(String user, String pass) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            User dbUser = session.createQuery("from User where username = :u", User.class)
                    .setParameter("u", user).uniqueResult();

            if (dbUser != null && PasswordHasher.checkPassword(pass, dbUser.getPassword())) {
                if (onlineUsers.containsKey(user)) {
                    out.println("LOGIN_RESPONSE|FAILED|Tài khoản này hiện đang đăng nhập ở một máy khác!");
                    return false;
                }
                out.println("LOGIN_RESPONSE|SUCCESS|Đăng nhập thành công!");
                return true;
            } else {
                out.println("LOGIN_RESPONSE|FAILED|Sai tên tài khoản hoặc mật khẩu đăng nhập!");
                return false;
            }
        } finally {
            session.close();
        }
    }

    private void handleChatMessage(String toUser, String type, String payload) {
        String dbContent = payload;
        String fileName = null;

        if (!"TEXT".equals(type)) {
            int sepIdx = payload.indexOf("|");
            if (sepIdx != -1) {
                fileName = payload.substring(0, sepIdx);
                dbContent = payload.substring(sepIdx + 1);
            }
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            ChatMessage msg = new ChatMessage(this.username, toUser, dbContent, type, fileName);
            session.save(msg);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("[SERVER - DATABASE LỖI] Không thể lưu tin nhắn: " + e.getMessage());
        } finally {
            session.close();
        }

        ClientHandler receiver = onlineUsers.get(toUser);
        if (receiver != null) {
            String outboundMsg = "INCOMING_MSG|" + this.username + "|" + type + "|" + payload;
            receiver.out.println(outboundMsg);
            receiver.out.flush(); // Bắt buộc giải phóng đường ống Socket lập tức
            System.out.println("[SERVER -> SOCKET] Đã trung chuyển trực tiếp tới @" + toUser);
        }
    }

    private void broadcastUserList() {
        String listStr = "USER_LIST|" + String.join(",", onlineUsers.keySet());
        for (ClientHandler handler : onlineUsers.values()) {
            handler.out.println(listStr);
            handler.out.flush();
        }
    }

    private void closeConnection() {
        if (username != null) {
            onlineUsers.remove(username);
            broadcastUserList();
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}