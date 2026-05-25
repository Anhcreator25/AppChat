package chat.client.gui;

import chat.client.network.ChatClientCore;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class ChatClientUI extends JFrame {
    private final String myUsername;
    private final ChatClientCore core;
    private String currentSelectedUser = null;

    private DefaultListModel<String> onlineListModel;
    private JList<String> onlineJList;

    private JLabel lblChatHeader;
    private JTextPane chatPane;
    private JTextField txtInput;
    private JButton btnSend;

    public ChatClientUI(String myUsername, ChatClientCore core) {
        this.myUsername = myUsername;
        this.core = core;

        setTitle("Tèo Coffee Chat - " + myUsername);
        setSize(850, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Khung bên trái (Danh sách bạn bè online)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(220, 550));
        leftPanel.setBackground(new Color(111, 78, 55));

        JLabel lblMyName = new JLabel("  " + myUsername.toUpperCase(), JLabel.LEFT);
        lblMyName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblMyName.setForeground(Color.WHITE);
        lblMyName.setPreferredSize(new Dimension(220, 45));
        leftPanel.add(lblMyName, BorderLayout.NORTH);

        onlineListModel = new DefaultListModel<>();
        onlineJList = new JList<>(onlineListModel);
        onlineJList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        onlineJList.setBackground(new Color(245, 239, 230));
        onlineJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        onlineJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = onlineJList.getSelectedValue();
                if (selected != null) {
                    currentSelectedUser = selected;
                    lblChatHeader.setText("Đang trò chuyện với: " + currentSelectedUser);
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(onlineJList);
        leftPanel.add(listScrollPane, BorderLayout.CENTER);
        add(leftPanel, BorderLayout.WEST);

        // Khung bên phải (Vùng hiển thị nội dung hội thoại)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);

        lblChatHeader = new JLabel("Hãy chọn một người dùng từ danh sách bên trái để bắt đầu chat", JLabel.LEFT);
        lblChatHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblChatHeader.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        rightPanel.add(lblChatHeader, BorderLayout.NORTH);

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatPane.setBackground(new Color(240, 240, 240));
        JScrollPane chatScrollPane = new JScrollPane(chatPane);
        rightPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Khung nhập tin nhắn phía dưới cùng
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        bottomPanel.setBackground(Color.WHITE);

        txtInput = new JTextField();
        txtInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSendMessage();
                }
            }
        });
        bottomPanel.add(txtInput, BorderLayout.CENTER);

        btnSend = new JButton("GỬI");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSend.setBackground(new Color(111, 78, 55));
        btnSend.setForeground(Color.WHITE);
        btnSend.setPreferredSize(new Dimension(80, 30));
        btnSend.addActionListener(e -> performSendMessage());
        bottomPanel.add(btnSend, BorderLayout.EAST);

        rightPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.CENTER);

        setupNetworkListeners();
    }

    private void performSendMessage() {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty()) return;

        if (currentSelectedUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người trong danh sách online để chat!");
            return;
        }

        core.sendPrivateMessage(currentSelectedUser, msg);
        appendMessageBubble(myUsername, msg, true);

        txtInput.setText("");
        txtInput.requestFocus();
    }

    private void setupNetworkListeners() {
        // 1. NHẬN DANH SÁCH ONLINE - ÉP REDRAW LÀM TƯƠI MÀN HÌNH
        core.addUserListListener(users -> SwingUtilities.invokeLater(() -> {
            onlineListModel.clear();
            for (String user : users) {
                if (!user.equals(myUsername)) {
                    onlineListModel.addElement(user);
                }
            }
            onlineJList.repaint();
            onlineJList.revalidate();
        }));

        // 2. NHẬN TIN NHẮN ĐẾN - BỎ QUA CHECK IF ĐỂ ĐẢM BẢO THÔNG LUỒNG VẼ TIN
        core.addPrivateMessageListener((fromUser, formattedMsg) -> SwingUtilities.invokeLater(() -> {
            appendMessageBubble(fromUser, formattedMsg, false);
            chatPane.setCaretPosition(chatPane.getDocument().getLength());
            chatPane.repaint();
        }));
    }

    private void appendMessageBubble(String sender, String message, boolean isMe) {
        try {
            javax.swing.text.StyledDocument doc = chatPane.getStyledDocument();
            javax.swing.text.SimpleAttributeSet attrs = new javax.swing.text.SimpleAttributeSet();

            if (isMe) {
                javax.swing.text.StyleConstants.setForeground(attrs, new Color(111, 78, 55));
                doc.insertString(doc.getLength(), "[Bạn]: " + message + "\n", attrs);
            } else {
                javax.swing.text.StyleConstants.setForeground(attrs, new Color(20, 80, 140));
                doc.insertString(doc.getLength(), "[" + sender + "]: " + message + "\n", attrs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}