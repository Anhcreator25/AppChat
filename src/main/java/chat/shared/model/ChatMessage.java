package chat.shared.model;

import jakarta.persistence.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "from_user", nullable = false, length = 50)
    private String fromUser;

    @Column(name = "to_user", nullable = false, length = 50)
    private String toUser;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content; // Lưu văn bản thô hoặc chuỗi Base64 của file/ảnh

    @Column(name = "msg_type", nullable = false, length = 20)
    private String msgType; // TEXT, IMAGE, VIDEO, FILE

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();

    public ChatMessage() {}

    public ChatMessage(String fromUser, String toUser, String content, String msgType, String fileName) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.content = content;
        this.msgType = msgType;
        this.fileName = fileName;
    }

    public int getId() { return id; }
    public String getFromUser() { return fromUser; }
    public String getToUser() { return toUser; }
    public String getContent() { return content; }
    public String getMsgType() { return msgType; }
    public String getFileName() { return fileName; }
    public LocalDateTime getTimestamp() { return timestamp; }
}