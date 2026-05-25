package chat.client.gui;

import chat.client.network.ChatClientCore;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class ChatClientUI extends JFrame {
    private final String myUsername;
    private final ChatClientCore core;
    private String currentSelectedUser = null;

    // Phân trang lịch sử chat
    private int historyOffset = 0;
    private static final int PAGE_SIZE = 30;
    private int receivedHistoryCount = 0;
    private boolean loadingHistory = false;
    private boolean allHistoryLoaded = false;

    // Sử dụng Model đối tượng tùy biến thay vì String đơn thuần
    private DefaultListModel<ContactItem> onlineListModel;
    private JList<ContactItem> onlineJList;
    private final Map<String, ContactItem> contactsMap = new HashMap<>();

    private JLabel lblChatHeader;
    private JPanel chatBoxContainer;
    private JScrollPane chatScrollPane;
    private JTextArea txtInput;
    private JButton btnSend;
    private JButton btnAttach;
    private JButton btnEmoji;

    // --- BẢNG MÀU MESSENGER PREMIUM ---
    private static final Color PRIMARY_BLUE = new Color(0, 132, 255);
    private static final Color BACKGROUND_LEFT = Color.WHITE;
    private static final Color BACKGROUND_RIGHT = new Color(240, 242, 245);
    private static final Color TEXT_MAIN = new Color(5, 5, 5);
    private static final Color TEXT_MUTED = new Color(101, 103, 107);
    private static final Color BUBBLE_ME = new Color(212, 230, 255);
    private static final Color BORDER_COLOR = new Color(240, 240, 240);
    private static final Color SEARCH_BG = new Color(240, 242, 245);

    // Kích thước tối đa cho bubble tin nhắn văn bản (Đo lường theo CM sang Pixel hợp lý)
    private static final int TEXT_BUBBLE_MAX_WIDTH_CM = 12;
    private static final int TEXT_BUBBLE_MAX_WIDTH_PX =
            (int) Math.round(TEXT_BUBBLE_MAX_WIDTH_CM *
                    Toolkit.getDefaultToolkit().getScreenResolution() / 2.54);


    // Lớp chứa dữ liệu cho một dòng liên hệ (Đầy đủ thuộc tính như ảnh mẫu)
    public static class ContactItem {
        String username;
        String lastMessage;
        String timeAgo;
        boolean isOnline;

        public ContactItem(String username, String lastMessage, String timeAgo, boolean isOnline) {
            this.username = username;
            this.lastMessage = lastMessage;
            this.timeAgo = timeAgo;
            this.isOnline = isOnline;
        }
    }

    public ChatClientUI(String myUsername, ChatClientCore core) {
        this.myUsername = myUsername;
        this.core = core;

        setupNetworkListeners();

        setTitle("Messenger - " + myUsername);
        setSize(1050, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // =========================================================================
        // 1. SIDEBAR BÊN TRÁI (THIẾT KẾ CHUẨN MESSENGER)
        // =========================================================================
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(340, 750));
        leftPanel.setBackground(BACKGROUND_LEFT);
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));

        // KHU VỰC HEADER SIDEBAR (Tên góc, Nút chức năng, Tìm kiếm, Tab phân loại)
        JPanel leftHeaderContainer = new JPanel();
        leftHeaderContainer.setLayout(new BoxLayout(leftHeaderContainer, BoxLayout.Y_AXIS));
        leftHeaderContainer.setBackground(BACKGROUND_LEFT);
        leftHeaderContainer.setBorder(new EmptyBorder(15, 16, 10, 16));

        // Hàng 1: Chữ "Đoạn chat" và các nút góc phải
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(BACKGROUND_LEFT);
        JLabel lblSidebarTitle = new JLabel("Đoạn chat");
        lblSidebarTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblSidebarTitle.setForeground(TEXT_MAIN);
        titleRow.add(lblSidebarTitle, BorderLayout.WEST);

        // Cụm icon chức năng tròn (Menu, Viết tin mới)
        JPanel iconActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        iconActionPanel.setBackground(BACKGROUND_LEFT);
        iconActionPanel.add(createCircleActionButton("•••"));
        iconActionPanel.add(createCircleActionButton("📝"));
        titleRow.add(iconActionPanel, BorderLayout.EAST);
        leftHeaderContainer.add(titleRow);
        leftHeaderContainer.add(Box.createVerticalStrut(12));

        // Hàng 2: Thanh tìm kiếm bo tròn (Search Bar)
        JTextField txtSearch = new JTextField("Tìm kiếm trên Messenger");
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setForeground(TEXT_MUTED);
        txtSearch.setBorder(null);
        txtSearch.setOpaque(false);

        JPanel searchWrapper = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(SEARCH_BG);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.dispose();
            }
        };
        searchWrapper.setOpaque(false);
        searchWrapper.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel lblSearchIcon = new JLabel("🔍");
        lblSearchIcon.setForeground(TEXT_MUTED);
        searchWrapper.add(lblSearchIcon, BorderLayout.WEST);
        searchWrapper.add(txtSearch, BorderLayout.CENTER);
        leftHeaderContainer.add(searchWrapper);
        leftHeaderContainer.add(Box.createVerticalStrut(12));

        // Hàng 3: Các Tab phân loại (Tất cả, Chưa đọc, Nhóm)
        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabRow.setBackground(BACKGROUND_LEFT);
        tabRow.add(createPillTabButton("Tất cả", true));
        tabRow.add(createPillTabButton("Chưa đọc", false));
        tabRow.add(createPillTabButton("Nhóm", false));
        leftHeaderContainer.add(tabRow);
        leftHeaderContainer.add(Box.createVerticalStrut(10));

        leftPanel.add(leftHeaderContainer, BorderLayout.NORTH);

        // DANH SÁCH CHAT SỬ DỤNG CELL RENDERER ĐỂ VẼ AVATAR TRÒN VÀ TIN NHẮN PHỤ
        onlineListModel = new DefaultListModel<>();
        onlineJList = new JList<>(onlineListModel);
        onlineJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineJList.setFixedCellHeight(72);
        onlineJList.setBackground(BACKGROUND_LEFT);
        onlineJList.setSelectionBackground(new Color(242, 244, 247));
        onlineJList.setSelectionForeground(TEXT_MAIN);

        onlineJList.setCellRenderer(new ListCellRenderer<ContactItem>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends ContactItem> list, ContactItem value, int index, boolean isSelected, boolean cellHasFocus) {
                JPanel itemPanel = new JPanel(new BorderLayout(12, 0));
                itemPanel.setOpaque(true);
                itemPanel.setBackground(isSelected ? new Color(242, 244, 247) : BACKGROUND_LEFT);
                itemPanel.setBorder(new EmptyBorder(0, 16, 0, 16));

                // Khối vẽ Avatar tròn có chấm xanh trạng thái sát viền (Góc trái)
                JPanel avatarBlock = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        g2d.setColor(new Color(225, 227, 230));
                        g2d.fill(new Ellipse2D.Double(0, 4, 44, 44));

                        g2d.setColor(TEXT_MUTED);
                        g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                        String initial = (value.username == null || value.username.isEmpty()) ? "?" : value.username.substring(0, 1).toUpperCase();
                        FontMetrics fm = g2d.getFontMetrics();
                        int textX = (44 - fm.stringWidth(initial)) / 2;
                        int textY = ((44 - fm.getHeight()) / 2) + fm.getAscent() + 4;
                        g2d.drawString(initial, textX, textY);

                        if (value.isOnline) {
                            g2d.setColor(Color.WHITE);
                            g2d.fill(new Ellipse2D.Double(30, 34, 15, 15));
                            g2d.setColor(new Color(49, 162, 76));
                            g2d.fill(new Ellipse2D.Double(32, 36, 11, 11));
                        }
                        g2d.dispose();
                    }
                };
                avatarBlock.setOpaque(false);
                avatarBlock.setPreferredSize(new Dimension(46, 52));
                itemPanel.add(avatarBlock, BorderLayout.WEST);

                // Khối hiển thị văn bản (Tên, nội dung chat, thời gian) ở trung tâm
                JPanel centerTextPanel = new JPanel();
                centerTextPanel.setLayout(new BoxLayout(centerTextPanel, BoxLayout.Y_AXIS));
                centerTextPanel.setOpaque(false);
                centerTextPanel.add(Box.createVerticalStrut(14));

                JLabel lblName = new JLabel(value.username);
                lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));
                lblName.setForeground(TEXT_MAIN);
                centerTextPanel.add(lblName);
                centerTextPanel.add(Box.createVerticalStrut(2));

                JLabel lblSub = new JLabel(value.lastMessage + "  ·  " + value.timeAgo);
                lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                lblSub.setForeground(TEXT_MUTED);
                centerTextPanel.add(lblSub);

                itemPanel.add(centerTextPanel, BorderLayout.CENTER);
                return itemPanel;
            }
        });

        onlineJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ContactItem selectedItem = onlineJList.getSelectedValue();
                if (selectedItem != null && !selectedItem.username.equals(currentSelectedUser)) {
                    currentSelectedUser = selectedItem.username;
                    lblChatHeader.setText("👤  " + currentSelectedUser);

                    chatBoxContainer.removeAll();
                    chatBoxContainer.revalidate();
                    chatBoxContainer.repaint();

                    historyOffset = 0;
                    receivedHistoryCount = 0;
                    allHistoryLoaded = false;
                    loadingHistory = true;
                    core.requestLoadHistory(currentSelectedUser, historyOffset, PAGE_SIZE);
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(onlineJList);
        listScrollPane.setBorder(null);
        leftPanel.add(listScrollPane, BorderLayout.CENTER);
        add(leftPanel, BorderLayout.WEST);

        // =========================================================================
        // 2. PANEL CHAT BÊN PHẢI (THIẾT KẾ KHUNG KHÔNG GIAN TRÒN)
        // =========================================================================
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(BACKGROUND_RIGHT);

        JPanel rightHeader = new JPanel(new BorderLayout());
        rightHeader.setBackground(Color.WHITE);
        rightHeader.setPreferredSize(new Dimension(710, 65));
        rightHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        lblChatHeader = new JLabel("Chọn một người ở danh sách để bắt đầu trò chuyện", JLabel.LEFT);
        lblChatHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblChatHeader.setForeground(TEXT_MAIN);
        lblChatHeader.setBorder(new EmptyBorder(0, 24, 0, 0));
        rightHeader.add(lblChatHeader, BorderLayout.CENTER);
        rightPanel.add(rightHeader, BorderLayout.NORTH);

        chatBoxContainer = new JPanel();
        chatBoxContainer.setLayout(new BoxLayout(chatBoxContainer, BoxLayout.Y_AXIS));
        chatBoxContainer.setBackground(BACKGROUND_RIGHT);
        chatBoxContainer.setBorder(new EmptyBorder(24, 24, 24, 24));

        chatScrollPane = new JScrollPane(chatBoxContainer);
        chatScrollPane.setBorder(null);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(25);
        rightPanel.add(chatScrollPane, BorderLayout.CENTER);

        // =========================================================================
        // 3. KHU VỰC NHẬP TIN NHẮN (BOTTOM PANEL)
        // =========================================================================
        JPanel bottomAreaPanel = new JPanel(new BorderLayout());
        bottomAreaPanel.setBackground(Color.WHITE);
        bottomAreaPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        JPanel inputRowPanel = new JPanel(new BorderLayout(15, 0));
        inputRowPanel.setBackground(Color.WHITE);
        inputRowPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel leftActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActionPanel.setBackground(Color.WHITE);

        btnAttach = createStyledToolbarButton("📎");
        btnAttach.addActionListener(e -> handleFileAttach());
        leftActionPanel.add(btnAttach);

        btnEmoji = createStyledToolbarButton("😀");
        btnEmoji.addActionListener(e -> handleEmojiSelect());
        leftActionPanel.add(btnEmoji);

        inputRowPanel.add(leftActionPanel, BorderLayout.WEST);

        txtInput = new JTextArea(1, 30);
        txtInput.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtInput.setLineWrap(true);
        txtInput.setWrapStyleWord(true);
        txtInput.setBorder(null);

        JPanel textInputWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(SEARCH_BG);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.dispose();
            }
        };
        textInputWrapper.setOpaque(false);
        textInputWrapper.setBorder(new EmptyBorder(10, 15, 10, 15));

        txtInput.setOpaque(false);
        textInputWrapper.add(txtInput, BorderLayout.CENTER);
        inputRowPanel.add(textInputWrapper, BorderLayout.CENTER);

        txtInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    performSendMessage();
                }
            }
        });

        btnSend = new JButton("Gửi") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSend.setBackground(PRIMARY_BLUE);
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusPainted(false);
        btnSend.setContentAreaFilled(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.setBorder(new EmptyBorder(0, 24, 0, 24));
        btnSend.addActionListener(e -> performSendMessage());
        inputRowPanel.add(btnSend, BorderLayout.EAST);

        bottomAreaPanel.add(inputRowPanel, BorderLayout.CENTER);
        rightPanel.add(bottomAreaPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);
        core.requestContacts();

        chatScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (e.getValueIsAdjusting()) return;
            JScrollBar sb = chatScrollPane.getVerticalScrollBar();
            int max = sb.getMaximum() - sb.getVisibleAmount();
            if (!loadingHistory && !allHistoryLoaded && sb.getValue() >= max && currentSelectedUser != null) {
                loadingHistory = true;
                core.requestLoadHistory(currentSelectedUser, historyOffset, PAGE_SIZE);
            }
        });
    }

    private JButton createCircleActionButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(240, 242, 245));
                g2d.fillOval(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, text.length() > 1 ? 11 : 14));
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(TEXT_MAIN);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createPillTabButton(String text, boolean isActive) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(isActive ? new Color(231, 243, 255) : Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(isActive ? PRIMARY_BLUE : TEXT_MUTED);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createStyledToolbarButton(String iconText) {
        JButton btn = new JButton(iconText);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btn.setPreferredSize(new Dimension(40, 40));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setForeground(TEXT_MUTED);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setOpaque(true);
                btn.setBackground(SEARCH_BG);
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
 * Sends a text message entered in the input area to the currently selected chat partner.
 * Validates that the message is not empty and that a contact is selected.
 * After sending, the message bubble is added to the UI and the input field is cleared.
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
 * Handles the attachment of a file (image, video, or generic file) to the current chat.
 * Opens a file chooser, determines the file type, reads the file bytes,
 * sends it through the core, and displays an appropriate bubble (image or file).
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
 * Opens a dialog to let the user select an emoji from a predefined list.
 * The chosen emoji is sent as an icon message and displayed as a text bubble.
 */
private void handleEmojiSelect() {
        if (currentSelectedUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người để gửi biểu tượng!");
            return;
        }
        String[] emojis = {"😀","😂","😍","👍","🎉","❤️","😎","🙏","😭","😮"};
        String selected = (String) JOptionPane.showInputDialog(
                this, "Chọn biểu tượng cảm xúc:", "Premium Emojis",
                JOptionPane.PLAIN_MESSAGE, null, emojis, emojis[0]
        );
        if (selected != null) {
            core.sendIconMessage(currentSelectedUser, selected);
            appendChatBubble(selected, true);
        }
    }

    /**
 * Registers all network listeners with {@link ChatClientCore}.
 *   • contactsListener – populates the contacts map with users the current user has chatted with.
 *   • userListListener – updates online/offline status for contacts and adds newly online users.
 *   • privateMessageListener – receives private messages and forwards them to UI.
 *   • historyMessageListener – receives historical messages for pagination.
 *   • historyDoneListener – marks the end of a page of history.
 */
private void setupNetworkListeners() {
        core.addContactsListener(users -> SwingUtilities.invokeLater(() -> {
            for (String user : users) {
                if (!user.equals(myUsername) && !contactsMap.containsKey(user)) {
                    ContactItem ci = new ContactItem(user, "Nhấp để trò chuyện...", "1 phút", false);
                    contactsMap.put(user, ci);
                    onlineListModel.addElement(ci);
                }
            }
            onlineJList.revalidate();
            onlineJList.repaint();
        }));

        core.addUserListListener(onlineUsers -> SwingUtilities.invokeLater(() -> {
            Set<String> onlineSet = new HashSet<>(onlineUsers);
            for (ContactItem ci : contactsMap.values()) {
                ci.isOnline = onlineSet.contains(ci.username);
            }
            for (String user : onlineSet) {
                if (!user.equals(myUsername) && !contactsMap.containsKey(user)) {
                    ContactItem ci = new ContactItem(user, "Nhấp để trò chuyện...", "1 phút", true);
                    contactsMap.put(user, ci);
                    onlineListModel.addElement(ci);
                }
            }
            onlineJList.revalidate();
            onlineJList.repaint();
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
            receivedHistoryCount++;
        }));

        core.addHistoryDoneListener(withUser -> SwingUtilities.invokeLater(() -> {
            if (receivedHistoryCount < PAGE_SIZE) {
                allHistoryLoaded = true;
            } else {
                historyOffset += PAGE_SIZE;
            }
            receivedHistoryCount = 0;
            loadingHistory = false;

            chatBoxContainer.revalidate();
            chatBoxContainer.repaint();
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        }));
    }

    /**
 * Parses a formatted message received from the server and routes it to the appropriate UI bubble.
 * Supported prefixes: [IMAGE]:, [FILE]:, [VIDEO]:. Other messages are treated as plain text.
 * @param formattedMsg The message payload from the server.
 * @param isMe         True if the message originates from the current user.
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

    // =========================================================================
    // KHU VỰC SỬA LỖI CHÍNH: TỰ ĐỘNG CO GIÃN THEO TEXT & BO TRÒN ĐẸP MẮT
    // =========================================================================
    /**
 * Creates and adds a text chat bubble to the conversation view.
 * The bubble width is limited to a maximum of 3 cm (converted to pixels).
 * The height is automatically calculated based on the wrapped text.
 * @param text The message text to display.
 * @param isMe True if the message was sent by the current user.
 */
private void appendChatBubble(String text, boolean isMe) {
        Box row = Box.createHorizontalBox();

        JTextArea bubbleArea = new JTextArea(text);
        bubbleArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        bubbleArea.setForeground(isMe ? Color.BLACK : TEXT_MAIN);
        bubbleArea.setEditable(false);
        bubbleArea.setLineWrap(true);
        bubbleArea.setWrapStyleWord(true);
        bubbleArea.setOpaque(false);
        // Đặt lề trong cho bong bóng chat chữ: Trên=10, Trái=16, Dưới=10, Phải=16
        bubbleArea.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel bubblePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                // Vẽ góc bo tròn 24px siêu mượt chuẩn phong cách Messenger hiện đại
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2d.dispose();
            }
        };
        bubblePanel.setOpaque(false);
        bubblePanel.add(bubbleArea, BorderLayout.CENTER);

        // THUẬT TOÁN ĐO ĐẠC KÍCH THƯỚC ĐỘNG TỰ ĐỘNG KHÍT THEO CHỮ KHÔNG PHÌNH DỌC
        int maxWidth = TEXT_BUBBLE_MAX_WIDTH_PX;
        FontMetrics fm = bubbleArea.getFontMetrics(bubbleArea.getFont());
        int calculatedTextWidth = fm.stringWidth(text) + 36; // Cộng bù trừ độ rộng khoảng trống padding
        int finalWidth = Math.min(calculatedTextWidth, maxWidth);

        // Đồng bộ chiều rộng tạm thời để JTextArea tự tính toán lại chiều cao chuẩn khi nhảy dòng
        bubbleArea.setSize(new Dimension(finalWidth, Integer.MAX_VALUE));
        int finalHeight = bubbleArea.getPreferredSize().height;

        // Ép cố định bộ kích thước, chặn hoàn toàn cơ chế tự co giãn bóp méo dọc của BoxLayout
        Dimension exactBubbleSize = new Dimension(finalWidth, finalHeight);
        bubblePanel.setPreferredSize(exactBubbleSize);
        bubblePanel.setMaximumSize(exactBubbleSize);
        bubblePanel.setMinimumSize(exactBubbleSize);

        if (isMe) {
            bubblePanel.setBackground(BUBBLE_ME);
            row.add(Box.createHorizontalGlue());
            row.add(bubblePanel);
            row.add(Box.createHorizontalStrut(4)); // Tạo khoảng cách nhẹ so với viền mép phải
        } else {
            bubblePanel.setBackground(Color.WHITE);
            row.add(Box.createHorizontalStrut(4)); // Tạo khoảng cách nhẹ so với viền mép trái
            row.add(bubblePanel);
            row.add(Box.createHorizontalGlue());
        }

        chatBoxContainer.add(row);
        chatBoxContainer.add(Box.createVerticalStrut(10)); // Khoảng cách giãn giữa các tin nhắn liền kề
        chatBoxContainer.revalidate();
        chatBoxContainer.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void appendImageBubble(ImageIcon icon, boolean isMe) {
        int maxWidth = 320;
        if (icon.getIconWidth() > maxWidth) {
            Image scaled = icon.getImage().getScaledInstance(maxWidth, -1, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaled);
        }

        Box row = Box.createHorizontalBox();
        JLabel imgLabel = new JLabel(icon);

        // Panel thông minh vẽ nền bo tròn bọc toàn bộ khối ảnh góc 20px
        JPanel bubblePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.dispose();
            }
        };
        bubblePanel.setOpaque(false);
        bubblePanel.add(imgLabel, BorderLayout.CENTER);
        bubblePanel.setBorder(new EmptyBorder(3, 3, 3, 3)); // Padding viền ảnh tinh tế

        // Khóa chiều cao tối đa cho ảnh chat để không bị BoxLayout Y_AXIS kéo giãn dọc màn hình
        Dimension imgSize = new Dimension(icon.getIconWidth() + 6, icon.getIconHeight() + 6);
        bubblePanel.setPreferredSize(imgSize);
        bubblePanel.setMaximumSize(imgSize);
        bubblePanel.setMinimumSize(imgSize);

        if (isMe) {
            bubblePanel.setBackground(BUBBLE_ME);
            row.add(Box.createHorizontalGlue());
            row.add(bubblePanel);
            row.add(Box.createHorizontalStrut(4));
        } else {
            bubblePanel.setBackground(Color.WHITE);
            row.add(Box.createHorizontalStrut(4));
            row.add(bubblePanel);
            row.add(Box.createHorizontalGlue());
        }

        chatBoxContainer.add(row);
        chatBoxContainer.add(Box.createVerticalStrut(10));
        chatBoxContainer.revalidate();
        chatBoxContainer.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void appendFileBubble(String fileName, byte[] data, boolean isMe) {
        try {
            File tempFile = File.createTempFile("zalo_file_", "_" + fileName);
            Files.write(tempFile.toPath(), data, StandardOpenOption.CREATE);
            tempFile.deleteOnExit();

            Box row = Box.createHorizontalBox();

            JPanel bubblePanel = new JPanel(new BorderLayout(15, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(getBackground());
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                    g2d.dispose();
                }
            };
            bubblePanel.setOpaque(false);
            bubblePanel.setBackground(Color.WHITE);
            bubblePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(225, 228, 232), 1, true),
                    new EmptyBorder(12, 16, 12, 16)
            ));

            String lowerName = fileName.toLowerCase();
            String fileIconSymbol = "📄";
            Color iconBgColor = new Color(140, 150, 160);

            if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
                fileIconSymbol = "DOC";
                iconBgColor = new Color(43, 87, 154);
            } else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                fileIconSymbol = "XLS";
                iconBgColor = new Color(33, 115, 70);
            } else if (lowerName.endsWith(".pdf")) {
                fileIconSymbol = "PDF";
                iconBgColor = new Color(222, 54, 32);
            }

            final String finalSymbol = fileIconSymbol;
            final Color finalIconBgColor = iconBgColor;
            JPanel iconBlock = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(finalIconBgColor);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2d.dispose();
                }
            };
            iconBlock.setOpaque(false);
            iconBlock.setPreferredSize(new Dimension(48, 48));

            JLabel lblSymbol = new JLabel(finalSymbol);
            lblSymbol.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblSymbol.setForeground(Color.WHITE);
            iconBlock.add(lblSymbol);
            bubblePanel.add(iconBlock, BorderLayout.WEST);

            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            JLabel lblFileName = new JLabel(fileName);
            lblFileName.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblFileName.setForeground(TEXT_MAIN);
            infoPanel.add(lblFileName);
            infoPanel.add(Box.createVerticalStrut(4));

            String sizeText = (data.length / 1024 > 0) ? (data.length / 1024) + " KB" : "1 KB";
            JLabel lblMeta = new JLabel(sizeText + "  •  Sẵn sàng");
            lblMeta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblMeta.setForeground(new Color(52, 168, 83));
            infoPanel.add(lblMeta);

            bubblePanel.add(infoPanel, BorderLayout.CENTER);

            JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            actionsPanel.setOpaque(false);

            JButton btnOpenFolder = createStyledToolbarButton("📁");
            btnOpenFolder.setToolTipText("Mở thư mục");
            btnOpenFolder.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(tempFile.getParentFile());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            actionsPanel.add(btnOpenFolder);

            JButton btnDownload = createStyledToolbarButton("📥");
            btnDownload.setToolTipText("Mở tệp tin");
            btnDownload.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(tempFile);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Không thể chạy tệp: " + ex.getMessage());
                }
            });
            actionsPanel.add(btnDownload);

            bubblePanel.add(actionsPanel, BorderLayout.EAST);

            // Ép kích thước khối File cố định tránh va vấp layout
            Dimension fileSize = new Dimension(420, 75);
            bubblePanel.setPreferredSize(fileSize);
            bubblePanel.setMaximumSize(fileSize);
            bubblePanel.setMinimumSize(fileSize);

            if (isMe) {
                row.add(Box.createHorizontalGlue());
                row.add(bubblePanel);
                row.add(Box.createHorizontalStrut(4));
            } else {
                row.add(Box.createHorizontalStrut(4));
                row.add(bubblePanel);
                row.add(Box.createHorizontalGlue());
            }

            chatBoxContainer.add(row);
            chatBoxContainer.add(Box.createVerticalStrut(14));
            chatBoxContainer.revalidate();
            chatBoxContainer.repaint();

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        } catch (IOException e) {
            appendChatBubble("[FILE]: " + fileName + " (Lỗi xử lý file)", isMe);
        }
    }
}