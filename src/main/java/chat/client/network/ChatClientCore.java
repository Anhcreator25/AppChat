package chat.client.network;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.Base64;
import java.nio.file.Files;

/**
 * Core class managing TCP socket connection to the chat server.
 * Handles sending and receiving protocol messages, and provides listener registration.
 */
public class ChatClientCore {
    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean isRunning = false;

    private Consumer<List<String>> userListListener;
    private Consumer<List<String>> contactListListener;
    private Consumer<List<String>> contactsListener; // danh bạ đã từng chat
    private BiConsumer<String, String> avatarListener;
    private BiConsumer<String, String> historyMessageListener;
    private Consumer<String> historyDoneListener;
    private BiConsumer<String, String> privateMessageListener;

    /**
 * Establishes a TCP connection to the chat server.
 *
 * @return true if the connection was successfully established, false otherwise.
 */
public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            isRunning = true;
            return true;
        } catch (IOException e) {
            System.err.println("[SOCKET CLIENT] Không thể kết nối tới máy chủ: " + e.getMessage());
            return false;
        }
    }

    /**
 * Sends an authentication action (LOGIN or REGISTER) to the server and returns the result.
 *
 * @param action either "LOGIN" or "REGISTER"
 * @param user   the username
 * @param pass   the password
 * @return a string starting with "SUCCESS:" or "FAILED:" followed by the server's message
 */
public String executeAuthAction(String action, String user, String pass) {
        try {
            out.println(action + "|" + user + "|" + pass);
            out.flush();

            String response = in.readLine();
            System.out.println("[SOCKET CLIENT] Nhận phản hồi gốc từ Server: " + response);

            if (response != null) {
                String[] tokens = response.split("\\|", 3);
                if (tokens.length >= 3) {
                    String status = tokens[1];
                    String message = tokens[2];

                    if ("SUCCESS".equals(status)) {
                        if ("LOGIN".equals(action)) {
                            this.username = user;
                        }
                        return "SUCCESS:" + message;
                    } else {
                        return "FAILED:" + message;
                    }
                }
            }
        } catch (IOException e) {
            return "FAILED:Mất kết nối với máy chủ trong quá trình xác thực!";
        }
        return "FAILED:Hệ thống phản hồi không hợp lệ.";
    }

    /**
 * Starts a background thread that continuously reads messages from the server
 * and dispatches them to the appropriate registered listeners.
 */
public void startListening() {
        new Thread(() -> {
            try {
                String rawLine;
                System.out.println("[SOCKET CLIENT] Luồng lắng nghe ngầm liên tục bắt đầu hoạt động...");
                while (isRunning && (rawLine = in.readLine()) != null) {
                    System.out.println("[SOCKET CLIENT] Gói mạng thô đổ về: " + rawLine);

                    String[] tokens = rawLine.split("\\|", 4);
                    if (tokens.length < 2) continue;

                    String cmd = tokens[0];
            if ("AVATAR".equals(cmd) && tokens.length >= 3) {
                String user = tokens[1];
                String base64 = tokens[2];
                if (avatarListener != null) {
                    avatarListener.accept(user, base64);
                }
            } else if ("USER_LIST".equals(cmd)) {
                List<String> list = new ArrayList<>();
                if (tokens.length >= 2 && !tokens[1].trim().isEmpty()) {
                    list.addAll(Arrays.asList(tokens[1].split(",")));
                }
                if (contactListListener != null) {
                    contactListListener.accept(list);
                } else if (userListListener != null) {
                    userListListener.accept(list);
                }
            } else if ("CONTACTS".equals(cmd) && tokens.length >= 2) {
                List<String> list = tokens[1].isEmpty() ? new ArrayList<>() : Arrays.asList(tokens[1].split(","));
                if (contactsListener != null) {
                    contactsListener.accept(list);
                }
            } else if ("INCOMING_MSG".equals(cmd) && tokens.length >= 4) {
                String fromUser = tokens[1];
                String type = tokens[2];
                String payload = tokens[3];

                String formattedUiMessage;
                if ("TEXT".equals(type)) {
                    formattedUiMessage = payload;
                } else {
                    formattedUiMessage = "[" + type + "]:" + payload;
                }

                if (privateMessageListener != null) {
                    System.out.println("[SOCKET CLIENT] Đẩy dữ liệu lên UI vẽ tin nhắn từ @" + fromUser);
                    privateMessageListener.accept(fromUser, formattedUiMessage);
                }
            } else if ("HISTORY_MSG".equals(cmd) && tokens.length >= 4) {
                String sender = tokens[1];
                String type = tokens[2];
                String payload = tokens[3];
                String formatted = "TEXT".equals(type) ? payload : "[" + type + "]:" + payload;
                if (historyMessageListener != null) {
                    historyMessageListener.accept(sender, formatted);
                }
            } else if ("HISTORY_DONE".equals(cmd) && tokens.length >= 2) {
                String user = tokens[1];
                if (historyDoneListener != null) {
                    historyDoneListener.accept(user);
                }
            }
            }
            } catch (IOException e) {
                System.out.println("[SOCKET CLIENT] Luồng lắng nghe ngầm đã dừng hoặc mất kết nối.");
            } finally {
                close();
            }
        }).start();
    }

    /**
 * Sends a private chat message or media to the specified user.
 *
 * @param toUser       the recipient username
 * @param formattedMsg message payload (plain text or prefixed with [IMAGE]:, [VIDEO]:, [FILE]:)
 */
public void sendPrivateMessage(String toUser, String formattedMsg) {
        if (out == null || !isRunning) {
            System.err.println("[SOCKET CLIENT LỖI] Không thể gửi tin, kết nối chưa sẵn sàng.");
            return;
        }

        String type = "TEXT";
        String payload = formattedMsg;

        if (formattedMsg.startsWith("[IMAGE]:")) {
            type = "IMAGE";
            payload = formattedMsg.substring(8);
        } else if (formattedMsg.startsWith("[VIDEO]:")) {
            type = "VIDEO";
            payload = formattedMsg.substring(8);
        } else if (formattedMsg.startsWith("[FILE]:")) {
            type = "FILE";
            payload = formattedMsg.substring(7);
        }

        String networkOutStr = "CHAT|" + toUser + "|" + type + "|" + payload;
        out.println(networkOutStr);
        out.flush();
        System.out.println("[SOCKET CLIENT -> SERVER] Đã đẩy gói tin gửi tới @" + toUser);
    }

    /**
 * Registers a listener that receives the list of online users.
 *
 * @param listener consumer that receives a list of usernames.
 */
public void addUserListListener(Consumer<List<String>> listener) {
        this.userListListener = listener;
        this.contactListListener = listener; // also set for UI alias
    }

    /**
     * Alias used by UI to receive online user list.
     */
    public void addContactListListener(Consumer<List<String>> listener) {
        this.contactListListener = listener;
    }

    /**
     * Register listener for contacts (users the current user has ever chatted with).
     */
    public void addContactsListener(Consumer<List<String>> listener) {
        this.contactsListener = listener;
    }

    /**
     * Register listener for avatar updates (username, base64 image).
     */
    public void addAvatarListener(BiConsumer<String, String> listener) {
        this.avatarListener = listener;
    }

    /**
 * Registers a listener for private messages received from other users.
 *
 * @param listener bi-consumer that receives (senderUsername, formattedMessage)
 */
public void addPrivateMessageListener(BiConsumer<String, String> listener) {
        this.privateMessageListener = listener;
    }

    /**
     * Register listener for history messages (sender, formattedMsg).
     */
    public void addHistoryMessageListener(BiConsumer<String, String> listener) {
        this.historyMessageListener = listener;
    }

    /**
     * Register listener that signals when the server finished sending history for a user.
     */
    public void addHistoryDoneListener(Consumer<String> listener) {
        this.historyDoneListener = listener;
    }

    /**
     * Request the server to send chat history with the specified user.
     */
    /**
     * Request chat history with default page size (30) starting from offset 0.
     */
    public void requestLoadHistory(String user) {
        requestLoadHistory(user, 0, 1000);
    }

    /**
     * Request chat history with pagination.
     * @param user   The other participant username.
     * @param offset Number of messages to skip (based on newest first ordering).
     * @param limit  Maximum number of messages to retrieve.
     */
    public void requestLoadHistory(String user, int offset, int limit) {
        if (out == null || !isRunning) return;
        out.println("GET_HISTORY|" + user + "|" + offset + "|" + limit);
        out.flush();
    }

/**
 * Sends a request to update the user's avatar on the server.
 *
 * @param base64 base64‑encoded image data representing the avatar
 */
    public void requestSetAvatar(String base64) {
        if (out == null || !isRunning) return;
        out.println("SET_AVATAR|" + base64);
        out.flush();
    }

    /**
 * Requests the server to send the list of contacts (users the current user has ever chatted with).
 */
public void requestContacts() {
        if (out == null || !isRunning) return;
        out.println("GET_CONTACTS");
        out.flush();
    }

    /**
     * Send a file (image, video, generic file) to a target user.
     * Payload format: <fileName>|<Base64Data>
     */
    /**
 * Sends a file (image, video, or generic document) to the specified user.
 *
 * @param toUser the recipient username
 * @param file   the file to send
 * @param type   message type: IMAGE, VIDEO, or FILE
 */
public void sendFileMessage(String toUser, File file, String type) {
        if (out == null || !isRunning) return;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String payload = file.getName() + "|" + base64;
            out.println("CHAT|" + toUser + "|" + type + "|" + payload);
            out.flush();
        } catch (IOException e) {
            System.err.println("[SOCKET CLIENT] File read error: " + e.getMessage());
        }
    }

    /**
     * Send an emoji/icon message.
     */
    /**
 * Sends an emoji/icon message to a user.
 *
 * @param toUser the recipient username
 * @param icon   the unicode emoji or icon string
 */
public void sendIconMessage(String toUser, String icon) {
        if (out == null || !isRunning) return;
        out.println("CHAT|" + toUser + "|ICON|" + icon);
        out.flush();
    }

    /**
 * Returns the username of the logged‑in user (if any).
 */
public String getUsername() {
        return username;
    }

    /**
 * Closes the socket and associated streams, terminating the client connection.
 */
public void close() {
        isRunning = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("[SOCKET CLIENT] Đã ngắt kết nối an toàn.");
        } catch (IOException ignored) {}
    }
}