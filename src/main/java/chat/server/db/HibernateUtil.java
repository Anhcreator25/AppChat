package chat.server.db;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Lớp tiện ích quản lý và cung cấp vòng đời kết nối SessionFactory của Hibernate.
 * Đảm bảo mô hình Singleton (chỉ tạo duy nhất 1 luồng kết nối dùng chung cho toàn Server).
 */
public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Tự động tìm và nạp cấu hình từ file src/main/resources/hibernate.cfg.xml
            return new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("[HIBERNATE UTIL - LỖI NGUY HIỂM] Khởi tạo SessionFactory thất bại!");
            System.err.println("Chi tiết lỗi: " + ex.getMessage());
            // In toàn bộ cây lỗi ra để dễ dàng debug nếu sai cấu hình hoặc MySQL chưa bật
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Lấy về SessionFactory duy nhất để mở kết nối truy vấn (Session)
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Đóng kết nối an toàn giải phóng bộ nhớ RAM và Socket của MySQL khi tắt Server
     */
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            getSessionFactory().close();
            System.out.println("[HIBERNATE UTIL] Đã đóng toàn bộ cổng kết nối Database an toàn.");
        }
    }
}