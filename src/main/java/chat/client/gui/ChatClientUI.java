package chat.client.gui;

import chat.client.network.ChatClientCore;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.io.File;

/**
 * Main UI window for the chat client.
 * Displays a list of contacts on the left and the conversation panel on the right.
 * Handles user interactions (sending messages, attaching files, emoji selection)
 * and forwards network actions to {@link ChatClientCore}.
 */
public class ChatClientUI extends JFrame {
    private final String myUsername; // Username of the logged‑in user
    private final ChatClientCore core; // Core responsible for network communication
    private String currentSelectedUser = null; // Currently selected chat partner (null when none)
    // Pagination state for chat history
    private int historyOffset = 0; // Offset for paginated history loading
    private static final int PAGE_SIZE = 30; // Number of messages per page when loading history
    private int receivedHistoryCount = 0; // Counter for messages received in the current page
    private boolean loadingHistory = false; // Flag indicating a history request is in progress
    private boolean allHistoryLoaded = false; // True when the entire chat history has been retrieved

    private DefaultListModel<String> onlineListModel;
    private JList<String> onlineJList;

    private JLabel lblChatHeader;
    private JPanel chatBoxContainer;
    private JScrollPane chatScrollPane;
    private JTextField txtInput;
    private JButton btnSend;
    private JButton btnAttach;
    private JButton btnEmoji;

    /**
 * Constructs the main chat window for a logged‑in user.
 *
 * @param myUsername the username of the logged‑in user
 * @param core       the network core used to send/receive messages
 */
public ChatClientUI(String myUsername, ChatClientCore core) {
        this.myUsername = myUsername;
        this.core = core;

        setupNetworkListeners();

        setTitle("Zalo Chat - " + myUsername);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // =========================================================================
        // 1. THANH DANH SÁCH TRÁI (DANH SÁCH BẠN BÈ)
        // =========================================================================
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(280, 650));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(225, 230, 235)));

        JPanel leftHeader = new JPanel(new BorderLayout());
        leftHeader.setBackground(new Color(0, 145, 255));
        leftHeader.setBorder(new EmptyBorder(0, 18, 0, 18));
        leftHeader.setPreferredSize(new Dimension(280, 60));

        JLabel lblMyName = new JLabel(myUsername.toUpperCase(), JLabel.LEFT);
        lblMyName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblMyName.setForeground(Color.WHITE);
        leftHeader.add(lblMyName, BorderLayout.CENTER);
        leftPanel.add(leftHeader, BorderLayout.NORTH);

        onlineListModel = new DefaultListModel<>();
        onlineJList = new JList<>(onlineListModel);
        onlineJList.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        onlineJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineJList.setFixedCellHeight(60);
        onlineJList.setBackground(Color.WHITE);
        onlineJList.setSelectionBackground(new Color(234, 245, 255));
        onlineJList.setSelectionForeground(new Color(0, 120, 242));

        onlineJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(new EmptyBorder(0, 20, 0, 0));
                label.setText("💬  " + value.toString());
                return label;
            }
        });

        onlineJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = onlineJList.getSelectedValue();
                if (selected != null && !selected.equals(currentSelectedUser)) {
                    currentSelectedUser = selected;
                    lblChatHeader.setText("👤  " + currentSelectedUser);

                    chatBoxContainer.removeAll();
                    chatBoxContainer.revalidate();
                    chatBoxContainer.repaint();

                    System.out.println("[UI LOG] Đang yêu cầu tải lịch sử chat với: " + currentSelectedUser);
                    // Reset pagination state
            historyOffset = 0;
            receivedHistoryCount = 0;
            allHistoryLoaded = false;
            loadingHistory = true;
            // Yêu cầu trang đầu (các tin cũ nhất)
            core.requestLoadHistory(currentSelectedUser, historyOffset, PAGE_SIZE);
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(onlineJList);
        listScrollPane.setBorder(null);
        leftPanel.add(listScrollPane, BorderLayout.CENTER);
        add(leftPanel, BorderLayout.WEST);

        // =========================================================================
        // 2. KHUNG HIỂN THỊ CHAT BÊN PHẢI
        // =========================================================================
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(238, 240, 242));

        JPanel rightHeader = new JPanel(new BorderLayout());
        rightHeader.setBackground(Color.WHITE);
        rightHeader.setPreferredSize(new Dimension(670, 60));
        rightHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 230, 235)));

        lblChatHeader = new JLabel("Chọn một người ở danh sách bên trái để bắt đầu cuộc trò chuyện", JLabel.LEFT);
        lblChatHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblChatHeader.setForeground(new Color(30, 35, 40));
        lblChatHeader.setBorder(new EmptyBorder(0, 25, 0, 0));
        rightHeader.add(lblChatHeader, BorderLayout.CENTER);
        rightPanel.add(rightHeader, BorderLayout.NORTH);

        chatBoxContainer = new JPanel();
        chatBoxContainer.setLayout(new BoxLayout(chatBoxContainer, BoxLayout.Y_AXIS));
        chatBoxContainer.setBackground(new Color(238, 240, 242));
        chatBoxContainer.setBorder(new EmptyBorder(20, 25, 20, 25));

        chatScrollPane = new JScrollPane(chatBoxContainer);
        chatScrollPane.setBorder(null);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        rightPanel.add(chatScrollPane, BorderLayout.CENTER);

        // =========================================================================
        // 3. KHUNG NHẬP LIỆU VÀ TIỆN ÍCH DƯỚI ĐÁY (Đã chuyển nút sang TRÁI ô nhập)
        // =========================================================================
        JPanel bottomAreaPanel = new JPanel(new BorderLayout());
        bottomAreaPanel.setBackground(Color.WHITE);
        bottomAreaPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 230, 235)));

        // Cấu trúc hàng nhập liệu chính
        JPanel inputRowPanel = new JPanel(new BorderLayout(12, 0));
        inputRowPanel.setBackground(Color.WHITE);
        inputRowPanel.setBorder(new EmptyBorder(12, 15, 12, 15));

        // KHU VỰC BÊN TRÁI: Chứa nút đính kèm file và nút Emoji (Đúng ý bạn)
        JPanel leftActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftActionPanel.setBackground(Color.WHITE);

        btnAttach = createStyledToolbarButton("\uD83D\uDCCE"); // Kẹp giấy
        btnAttach.addActionListener(e -> handleFileAttach());
        leftActionPanel.add(btnAttach);

        btnEmoji = createStyledToolbarButton("\uD83D\uDE00"); // Mặt cười
        btnEmoji.addActionListener(e -> handleEmojiSelect());
        leftActionPanel.add(btnEmoji);

        inputRowPanel.add(leftActionPanel, BorderLayout.WEST);

        // KHU VỰC TRUNG TÂM: Ô nhập liệu văn bản
        txtInput = new JTextField();
        txtInput.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 220, 225), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        txtInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSendMessage();
                }
            }
        });
        inputRowPanel.add(txtInput, BorderLayout.CENTER);

        // KHU VỰC BÊN PHẢI: Nút gửi tin nhắn xanh chủ đạo
        btnSend = new JButton("Gửi");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSend.setBackground(new Color(0, 145, 255));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusPainted(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 135, 240), 1, true),
                new EmptyBorder(0, 22, 0, 22)
        ));
        btnSend.addActionListener(e -> performSendMessage());
        inputRowPanel.add(btnSend, BorderLayout.EAST);

        bottomAreaPanel.add(inputRowPanel, BorderLayout.CENTER);
        rightPanel.add(bottomAreaPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);
        core.requestContacts(); // yêu cầu danh sách liên hệ sau khi UI khởi tạo
        // Thiết lập listener cuộn để tải thêm lịch sử khi cuộn tới cuối
        chatScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (e.getValueIsAdjusting()) return;
            JScrollBar sb = chatScrollPane.getVerticalScrollBar();
            int max = sb.getMaximum() - sb.getVisibleAmount(); // vị trí cuối cùng (có thể cuộn tới)
            if (!loadingHistory && !allHistoryLoaded && sb.getValue() >= max) {
                // Yêu cầu tải trang tiếp theo
                loadingHistory = true;
                core.requestLoadHistory(currentSelectedUser, historyOffset, PAGE_SIZE);
            }
        });
    }

    private JButton createStyledToolbarButton(String iconText) {
        JButton btn = new JButton(iconText);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btn.setPreferredSize(new Dimension(38, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setForeground(new Color(110, 125, 140));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setOpaque(true);
                btn.setBackground(new Color(242, 244, 247));
                btn.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
                btn.setBackground(new Color(0,0,0,0));
                btn.repaint();
            }
        });
        return btn;
    }

    /**
 * Sends the text currently typed in the input field to the selected user.
 * Validates that a user is selected and the message is non‑empty.
 */
private void performSendMessage() {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty()) return;

        if (currentSelectedUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người trong danh sách online để chat!");
            return;
        }

        core.sendPrivateMessage(currentSelectedUser, msg);
        appendChatBubble(msg, true);

        txtInput.setText("");
        txtInput.requestFocus();
    }

    /**
 * Opens a file chooser, determines the file type, reads the file bytes,
 * and sends the file/message to the selected user via {@link ChatClientCore}.
 * Also updates the UI with a preview (image or file bubble).
 */
private void handleFileAttach() {
        if (currentSelectedUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người để gửi tệp!");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int rc = chooser.showOpenDialog(this);
        if (rc == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String name = file.getName().toLowerCase();
            String type;
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
                type = "IMAGE";
            } else if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv") || name.endsWith(".mov")) {
                type = "VIDEO";
            } else {
                type = "FILE";
            }

            // Đọc dữ liệu thô gửi qua mạng
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                core.sendFileMessage(currentSelectedUser, file, type);

                if ("IMAGE".equals(type)) {
                    ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                    appendImageBubble(icon, true);
                } else {
                    appendFileBubble(file.getName(), bytes, true);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
 * Shows a simple dialog with a list of emojis.
 * When an emoji is selected, sends it as an ICON message and displays it in the chat.
 */
private void handleEmojiSelect() {
        if (currentSelectedUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người để gửi biểu tượng!");
            return;
        }
        String[] emojis = {"😀","😂","😍","👍","🎉","❤️","😎","🙏","😭","😮"};
        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Chọn biểu tượng cảm xúc:",
                "Zalo Emojis",
                JOptionPane.PLAIN_MESSAGE,
                null,
                emojis,
                emojis[0]
        );
        if (selected != null) {
            core.sendIconMessage(currentSelectedUser, selected);
            appendChatBubble(selected, true);
        }
    }

    /**
 * Registers all network event listeners with {@link ChatClientCore}.
 * Listeners update the UI when contacts, private messages, or history data arrive.
 */
private void setupNetworkListeners() {
        core.addContactsListener(users -> SwingUtilities.invokeLater(() -> {
            onlineListModel.clear();
            for (String user : users) {
                if (!user.equals(myUsername)) {
                    onlineListModel.addElement(user);
                }
            }
            onlineJList.repaint();
            onlineJList.revalidate();
        }));

        core.addPrivateMessageListener((fromUser, formattedMsg) -> SwingUtilities.invokeLater(() -> {
            if (currentSelectedUser == null) {
                currentSelectedUser = fromUser;
                lblChatHeader.setText("👤  " + currentSelectedUser);
            }
            if (fromUser.equals(currentSelectedUser)) {
                processIncomingFormattedMessage(formattedMsg, false);
            }
        }));

        core.addHistoryMessageListener((sender, formattedMsg) -> SwingUtilities.invokeLater(() -> {
            boolean isMe = sender.equals(myUsername);
            processIncomingFormattedMessage(formattedMsg, isMe);
            // Đếm số tin trong trang hiện tại
            receivedHistoryCount++;
        }));

core.addHistoryDoneListener(withUser -> SwingUtilities.invokeLater(() -> {
                // Khi một trang lịch sử đã được gửi xong
                if (receivedHistoryCount < PAGE_SIZE) {
                    // Không còn tin nào nữa
                    allHistoryLoaded = true;
                } else {
                    // Có thể còn tin, tăng offset để tải trang tiếp theo khi cần
                    historyOffset += PAGE_SIZE;
                }
                // Reset đếm cho trang tiếp theo
                receivedHistoryCount = 0;
                loadingHistory = false;

                chatBoxContainer.revalidate();
                chatBoxContainer.repaint();
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                // cuộn xuống cuối cùng (tin mới nhất)
                vertical.setValue(vertical.getMaximum());
            }));
    }

    /**
 * Parses a formatted message received from the server and forwards it to the appropriate UI element.
 *
 * @param formattedMsg the raw message string (may contain prefixes for images, files, video)
 * @param isMe         true if the sender is the local user, false otherwise
 */
private void processIncomingFormattedMessage(String formattedMsg, boolean isMe) {
        if (formattedMsg.startsWith("[IMAGE]:")) {
            String payload = formattedMsg.substring("[IMAGE]:".length());
            int sepIdx = payload.indexOf('|');
            if (sepIdx != -1) {
                String base64 = payload.substring(sepIdx + 1);
                byte[] data = Base64.getDecoder().decode(base64);
                ImageIcon icon = new ImageIcon(data);
                appendImageBubble(icon, isMe);
            } else {
                appendChatBubble(formattedMsg, isMe);
            }
        } else if (formattedMsg.startsWith("[FILE]:") || formattedMsg.startsWith("[VIDEO]:")) {
            String payload = formattedMsg.startsWith("[FILE]:") ?
                    formattedMsg.substring("[FILE]:".length()) : formattedMsg.substring("[VIDEO]:".length());
            int sepIdx = payload.indexOf('|');
            if (sepIdx != -1) {
                String fileName = payload.substring(0, sepIdx);
                String base64 = payload.substring(sepIdx + 1);
                byte[] data = Base64.getDecoder().decode(base64);
                appendFileBubble(fileName, data, isMe);
            } else {
                appendChatBubble(formattedMsg, isMe);
            }
        } else {
            appendChatBubble(formattedMsg, isMe);
        }
    }

    /**
 * Adds a text bubble to the chat area.
 *
 * @param text the message text to display
 * @param isMe true if the message was sent by the local user (right‑aligned)
 */
private void appendChatBubble(String text, boolean isMe) {
        Box row = Box.createHorizontalBox();

        JTextArea bubbleArea = new JTextArea(text);
        bubbleArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        bubbleArea.setEditable(false);
        bubbleArea.setLineWrap(true);
        bubbleArea.setWrapStyleWord(true);
        bubbleArea.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel bubblePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.dispose();
            }
        };
        bubblePanel.setOpaque(false);
        bubblePanel.add(bubbleArea, BorderLayout.CENTER);

        int maxWidth = 420;
        int preferredWidth = bubbleArea.getFontMetrics(bubbleArea.getFont()).stringWidth(text) + 35;
        bubblePanel.setMaximumSize(new Dimension(Math.min(preferredWidth, maxWidth), Integer.MAX_VALUE));

        if (isMe) {
            bubblePanel.setBackground(new Color(213, 236, 255));
            bubbleArea.setBackground(new Color(213, 236, 255));
            row.add(Box.createHorizontalGlue());
            row.add(bubblePanel);
        } else {
            bubblePanel.setBackground(Color.WHITE);
            bubbleArea.setBackground(Color.WHITE);
            row.add(bubblePanel);
            row.add(Box.createHorizontalGlue());
        }

        chatBoxContainer.add(row);
        chatBoxContainer.add(Box.createVerticalStrut(12));
        chatBoxContainer.revalidate();
        chatBoxContainer.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    /**
 * Adds an image bubble to the chat area.
 * The image is scaled down if it exceeds a maximum width.
 *
 * @param icon  the image to display
 * @param isMe  true if the image was sent by the local user (right‑aligned)
 */
private void appendImageBubble(ImageIcon icon, boolean isMe) {
        int maxWidth = 320;
        if (icon.getIconWidth() > maxWidth) {
            Image scaled = icon.getImage().getScaledInstance(maxWidth, -1, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaled);
        }

        Box row = Box.createHorizontalBox();
        JLabel imgLabel = new JLabel(icon);
        imgLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel bubblePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.dispose();
            }
        };
        bubblePanel.setOpaque(false);
        bubblePanel.add(imgLabel, BorderLayout.CENTER);
        bubblePanel.setMaximumSize(new Dimension(icon.getIconWidth() + 10, icon.getIconHeight() + 10));

        if (isMe) {
            bubblePanel.setBackground(new Color(213, 236, 255));
            row.add(Box.createHorizontalGlue());
            row.add(bubblePanel);
        } else {
            bubblePanel.setBackground(Color.WHITE);
            row.add(bubblePanel);
            row.add(Box.createHorizontalGlue());
        }

        chatBoxContainer.add(row);
        chatBoxContainer.add(Box.createVerticalStrut(12));
        chatBoxContainer.revalidate();
        chatBoxContainer.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    // =========================================================================
    // 4. BONG BÓNG HIỂN THỊ FILE CHUẨN ZALO (NHƯ ẢNH MINH HỌA)
    // =========================================================================
    /**
 * Creates a file bubble that mimics Zalo's file preview UI.
 * The file bytes are written to a temporary file for preview/download actions.
 *
 * @param fileName the original file name (used for icon selection and display)
 * @param data     the raw file data (Base64‑decoded)
 * @param isMe     true if the file was sent by the local user (right‑aligned)
 */
private void appendFileBubble(String fileName, byte[] data, boolean isMe) {
        try {
            // Khởi tạo file tạm thời lưu trữ ngầm
            File tempFile = File.createTempFile("zalo_file_", "_" + fileName);
            Files.write(tempFile.toPath(), data, StandardOpenOption.CREATE);
            tempFile.deleteOnExit();

            Box row = Box.createHorizontalBox();

            // Khung panel chính chứa toàn bộ cấu trúc file bo tròn góc mẫu Zalo
            JPanel bubblePanel = new JPanel(new BorderLayout(15, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(getBackground());
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); // Góc bo nhẹ cao cấp
                    g2d.dispose();
                }
            };
            bubblePanel.setOpaque(false);
            bubblePanel.setBackground(Color.WHITE); // Giữ nền trắng sạch sẽ chuẩn Zalo cho cả 2 bên
            bubblePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(218, 223, 230), 1, true),
                    new EmptyBorder(12, 14, 12, 14)
            ));

            // --- THÀNH PHẦN 1: ICON KHỐI FILE PHÍA TRÁI (Word, Excel, PDF, Chung) ---
            String lowerName = fileName.toLowerCase();
            String fileIconSymbol = "📄"; // Icon mặc định
            Color iconBgColor = new Color(110, 130, 145); // Màu xám mặc định

            if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
                fileIconSymbol = "W";
                iconBgColor = new Color(43, 87, 154); // Xanh Word chuẩn
            } else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                fileIconSymbol = "X";
                iconBgColor = new Color(33, 115, 70); // Xanh lá Excel
            } else if (lowerName.endsWith(".pdf")) {
                fileIconSymbol = "PDF";
                iconBgColor = new Color(222, 54, 32); // Đỏ PDF
            }

            final String finalSymbol = fileIconSymbol;
            final Color finalIconBgColor = iconBgColor;
            JPanel iconBlock = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(finalIconBgColor);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8); // Khối vuông bo góc
                    g2d.dispose();
                }
            };
            iconBlock.setOpaque(false);
            iconBlock.setPreferredSize(new Dimension(45, 45));

            JLabel lblSymbol = new JLabel(finalSymbol);
            lblSymbol.setFont(new Font("Segoe UI", Font.BOLD, finalSymbol.length() > 2 ? 11 : 16));
            lblSymbol.setForeground(Color.WHITE);
            iconBlock.add(lblSymbol);
            bubblePanel.add(iconBlock, BorderLayout.WEST);

            // --- THÀNH PHẦN 2: THÔNG TIN FILE (Tên file, Dung lượng, Trạng thái) ---
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            JLabel lblFileName = new JLabel(fileName);
            lblFileName.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblFileName.setForeground(new Color(30, 35, 40));
            infoPanel.add(lblFileName);
            infoPanel.add(Box.createVerticalStrut(3));

            // Tính toán dung lượng ảo cho đẹp mắt
            String sizeText = (data.length / 1024 > 0) ? (data.length / 1024) + " KB" : "1 KB";
            JLabel lblMeta = new JLabel(sizeText + "   •   ✓ Đã có trên máy");
            lblMeta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblMeta.setForeground(new Color(40, 167, 69)); // Trạng thái xanh lá cây tươi mát của Zalo
            infoPanel.add(lblMeta);

            bubblePanel.add(infoPanel, BorderLayout.CENTER);

            // --- THÀNH PHẦN 3: CỤM NÚT ĐIỀU KHIỂN BÊN PHẢI (Mở mục / Tải xuống) ---
            JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actionsPanel.setOpaque(false);

            // Nút 1: Mở thư mục chứa file tạm
            JButton btnOpenFolder = new JButton("📁");
            btnOpenFolder.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            btnOpenFolder.setContentAreaFilled(false);
            btnOpenFolder.setBorderPainted(false);
            btnOpenFolder.setFocusPainted(false);
            btnOpenFolder.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnOpenFolder.setToolTipText("Mở thư mục chứa file");
            btnOpenFolder.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(tempFile.getParentFile());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            actionsPanel.add(btnOpenFolder);

            // Nút 2: Click trực tiếp để mở file
            JButton btnDownload = new JButton("📥");
            btnDownload.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            btnDownload.setContentAreaFilled(false);
            btnDownload.setBorderPainted(false);
            btnDownload.setFocusPainted(false);
            btnDownload.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnDownload.setToolTipText("Mở chạy tệp tin nhanh");
            btnDownload.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(tempFile);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Không thể chạy tệp: " + ex.getMessage());
                }
            });
            actionsPanel.add(btnDownload);

            bubblePanel.add(actionsPanel, BorderLayout.EAST);
            bubblePanel.setMaximumSize(new Dimension(380, 70));

            // Căn lề trái / phải dựa theo người gửi cuộc thoại
            if (isMe) {
                row.add(Box.createHorizontalGlue());
                row.add(bubblePanel);
            } else {
                row.add(bubblePanel);
                row.add(Box.createHorizontalGlue());
            }

            chatBoxContainer.add(row);
            chatBoxContainer.add(Box.createVerticalStrut(12));
            chatBoxContainer.revalidate();
            chatBoxContainer.repaint();

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        } catch (IOException e) {
            appendChatBubble("[FILE]: " + fileName + " (Lỗi xử lý file tuần tự)", isMe);
        }
    }
}