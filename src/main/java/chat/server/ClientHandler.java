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

/**
 * Handles a single client connection, processing authentication,
 * chat messages, and history requests.
 * Runs in its own thread.
 */
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
    /**
 * Main execution loop: reads client requests, processes authentication,
 * then handles chat messages and history queries.
 */
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
                        // Send the full contacts list (users the current user has ever chatted with)
                        sendContactList();
                        break;
                    }
                }
            }

            if (this.username != null) {
                while ((requestLine = in.readLine()) != null) {
                    String[] parts = requestLine.split("\\|", 4);
                    if (parts.length < 1) continue;
                    String cmd = parts[0];
                    if ("CHAT".equals(cmd) && parts.length >= 4) {
                        String toUser = parts[1];
                        String type = parts[2];
                        String payload = parts[3];
                        handleChatMessage(toUser, type, payload);
                    } else if ("GET_HISTORY".equals(cmd)) {
                        // Format: GET_HISTORY|otherUser|offset|limit
                        String otherUser = parts.length > 1 ? parts[1] : null;
                        int offset = 0;
                        int limit = 30;
                        if (parts.length > 2) {
                            try { offset = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                        }
                        if (parts.length > 3) {
                            try { limit = Integer.parseInt(parts[3]); } catch (NumberFormatException ignored) {}
                        }
                        if (otherUser != null) {
                            handleHistoryRequest(otherUser, offset, limit);
                        }
                    } else if ("GET_CONTACTS".equals(cmd)) {
                        sendContactList();
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("[SERVER] Người dùng " + (username != null ? username : "Chưa đăng nhập") + " mất kết nối.");
        } finally {
            closeConnection();
        }
    }

    /**
 * Handles a REGISTER request: creates a new user record if the username is not taken.
 *
 * @param user the requested username
 * @param pass the plaintext password (will be hashed before storing)
 */
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

    /**
 * Handles a LOGIN request: verifies credentials and checks if the user is already online.
 *
 * @param user the username
 * @param pass the plaintext password
 * @return true if login succeeds, false otherwise
 */
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

    /**
 * Persists a chat message to the database and forwards it to the intended recipient if they are online.
 *
 * @param toUser   recipient username
 * @param type     message type (TEXT, IMAGE, VIDEO, FILE)
 * @param payload  raw payload (for files: fileName|Base64Data)
 */
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

    /**
     * Send a page of chat history to the requesting client.
     * Messages are ordered by timestamp DESC (newest first) and limited by offset/limit.
     * Each message is sent as: HISTORY_MSG|sender|type|payload
     * After all messages of the page are sent, a terminating marker is sent: HISTORY_DONE|otherUser
     */
    private void handleHistoryRequest(String otherUser, int offset, int limit) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String hql = "FROM ChatMessage m WHERE (m.fromUser = :me AND m.toUser = :other) OR (m.fromUser = :other AND m.toUser = :me) ORDER BY m.timestamp ASC";
            org.hibernate.query.Query<ChatMessage> query = session.createQuery(hql, ChatMessage.class);
            query.setParameter("me", username);
            query.setParameter("other", otherUser);
            query.setFirstResult(offset);
            query.setMaxResults(limit);
            java.util.List<ChatMessage> msgs = query.list();
            for (ChatMessage msg : msgs) {
                String type = msg.getMsgType();
                String payload;
                if ("TEXT".equals(type)) {
                    payload = msg.getContent();
                } else {
                    // payload format: fileName|Base64Data
                    payload = (msg.getFileName() != null ? msg.getFileName() : "") + "|" + msg.getContent();
                }
                String outStr = "HISTORY_MSG|" + msg.getFromUser() + "|" + type + "|" + payload;
                out.println(outStr);
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("[SERVER - DB] Lỗi khi lấy lịch sử: " + e.getMessage());
        } finally {
            session.close();
        }
        // Signal end of this page
        out.println("HISTORY_DONE|" + otherUser);
        out.flush();
    }

    /**
     * Send the full contacts list to the client (users ever chatted with).
     */
    private void sendContactList() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            String hql = "SELECT DISTINCT CASE WHEN m.fromUser = :me THEN m.toUser ELSE m.fromUser END FROM ChatMessage m WHERE m.fromUser = :me OR m.toUser = :me";
            org.hibernate.query.Query<String> q = session.createQuery(hql, String.class);
            q.setParameter("me", username);
            java.util.List<String> contacts = q.list();
            out.println("CONTACTS|" + String.join(",", contacts));
            out.flush();
        } finally {
            session.close();
        }
    }

    /**
 * Sends the current online user list to all connected clients.
 */
private void broadcastUserList() {
        String listStr = "USER_LIST|" + String.join(",", onlineUsers.keySet());
        for (ClientHandler handler : onlineUsers.values()) {
            handler.out.println(listStr);
            handler.out.flush();
        }
    }

    /**
 * Closes the client socket and removes the user from the online list.
 */
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