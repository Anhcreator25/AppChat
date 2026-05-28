package chat.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BotReplyService {

    public static String getReply(String dbContent) {
        try {
            // 1. Kết nối tới cổng cục bộ của Ollama
            URL url = new URL("http://localhost:11434/api/chat");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // 2. Ép sạch các ký tự đặc biệt từ giao diện để nhét vừa chuỗi JSON gửi đi
            String safePrompt = dbContent.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");

           String jsonInputString = String.format(
                    "{\"model\":\"llama3\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"stream\":false,\"options\":{\"num_predict\":8192}}",
                    safePrompt
            );

            // 3. Đẩy dữ liệu lên đường ống kết nối
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 4. Đọc luồng dữ liệu thô trả về từ AI
            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }

                    String jsonResponse = response.toString();
                    String keyword = "\"content\":\"";
                    int start = jsonResponse.indexOf(keyword);
                    if (start != -1) {
                        start += keyword.length();

                        // Tìm dấu nháy kép kết thúc trường content, bỏ qua các dấu nháy kép \" nằm trong code
                        int end = -1;
                        for (int i = start; i < jsonResponse.length(); i++) {
                            if (jsonResponse.charAt(i) == '"') {
                                // Kiểm tra xem dấu nháy kép này có bị gạch chéo bảo vệ không (\")
                                if (jsonResponse.charAt(i - 1) != '\\') {
                                    // Đảm bảo phía sau nó là dấu đóng ngoặc nhọn } hoặc dấu phẩy để chắc chắn kết thúc trường
                                    String trailing = jsonResponse.substring(i + 1, Math.min(i + 10, jsonResponse.length()));
                                    if (trailing.contains("}") || trailing.contains(",")) {
                                        end = i;
                                        break;
                                    }
                                }
                            }
                        }

                  if (end != -1) {
                        String aiAnswer = jsonResponse.substring(start, end);
                        // Escape pipe character to avoid client split issues
                        aiAnswer = aiAnswer.replace("|", "%7C");
                        return aiAnswer;
                    }
                    }
                    return "Không thể trích xuất nội dung tin nhắn thô từ mô hình!";
                }
            } else {
                return "[Hệ thống] Lỗi kết nối Ollama, mã lỗi: " + code;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "[Hệ thống] Không thể kết nối tới Ollama. Hãy đảm bảo CMD vẫn đang chạy!";
        }
    }
}