package chat.client.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ChatbotService {

    public static String ask(String userPrompt) {
        try {
            // 1. Tự động kiểm tra API Key từ cấu hình môi trường IntelliJ
            String apiKey = System.getenv("CHATBOT_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return "Lỗi: Chưa cấu hình biến môi trường CHATBOT_API_KEY trong IntelliJ!";
            }

            // 2. Sử dụng endpoint v1beta kết hợp dòng model ổn định cao nhất để sửa lỗi 404
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            // Xử lý chuỗi văn bản (escape JSON ký tự đặc biệt) để tránh lỗi định dạng gói tin gửi đi
            String sanitizedPrompt = userPrompt.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            // 3. Khung Body đúng chuẩn quy định cấu trúc tài liệu của Google API
            String jsonRequestBody = "{"
                    + "  \"contents\": [{"
                    + "    \"parts\":[{"
                    + "      \"text\": \"" + sanitizedPrompt + "\""
                    + "    }]"
                    + "  }]"
                    + "}";

            // 4. Khởi tạo bộ công cụ HTTP Client đồng bộ gói tin gửi đi
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Trích xuất xử lý kết quả phản hồi
            if (response.statusCode() == 200) {
                return parseGeminiResponse(response.body());
            } else {
                return "Google AI trả về mã lỗi: " + response.statusCode() + "\nChi tiết hệ thống:\n" + response.body();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi kết nối hệ thống Chatbot: " + e.getMessage();
        }
    }

    /**
     * Hàm bóc tách thủ công chuỗi để lấy phần text phản hồi từ cấu trúc JSON trả về của Gemini
     */
    private static String parseGeminiResponse(String responseBody) {
        try {
            String searchStr = "\"text\": \"";
            int startIdx = responseBody.indexOf(searchStr);
            if (startIdx != -1) {
                startIdx += searchStr.length();
                int endIdx = responseBody.indexOf("\"", startIdx);
                if (endIdx != -1) {
                    String result = responseBody.substring(startIdx, endIdx);
                    // Định dạng lại các ký tự xuống dòng và dấu nháy kép cho người dùng đọc
                    return result.replace("\\n", "\n").replace("\\\"", "\"");
                }
            }
            return "Đã nhận phản hồi từ AI nhưng không bóc tách được văn bản (Cấu trúc JSON trống).";
        } catch (Exception e) {
            return "Lỗi phân tích dữ liệu văn bản: " + e.getMessage();
        }
    }
}