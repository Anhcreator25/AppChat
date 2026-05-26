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

public void addUserListListener(Consumer<List<String>> listener) {
        this.userListListener = listener;
        this.contactListListener = listener; // also set for UI alias
    }


    public void addContactsListener(Consumer<List<String>> listener) {
        this.contactsListener = listener;
    }

public void addPrivateMessageListener(BiConsumer<String, String> listener) {
        this.privateMessageListener = listener;
    }


    public void addHistoryMessageListener(BiConsumer<String, String> listener) {
        this.historyMessageListener = listener;
    }


    public void addHistoryDoneListener(Consumer<String> listener) {
        this.historyDoneListener = listener;
    }
    public void requestLoadHistory(String user, int offset, int limit) {
        if (out == null || !isRunning) return;
        out.println("GET_HISTORY|" + user + "|" + offset + "|" + limit);
        out.flush();
    }

public void requestContacts() {
        if (out == null || !isRunning) return;
        out.println("GET_CONTACTS");
        out.flush();
    }

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

public void sendIconMessage(String toUser, String icon) {
        if (out == null || !isRunning) return;
        out.println("CHAT|" + toUser + "|ICON|" + icon);
        out.flush();
    }

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