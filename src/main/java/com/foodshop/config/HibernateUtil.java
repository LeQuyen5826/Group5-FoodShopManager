package com.foodshop.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            try {
                Configuration config = new Configuration();
                config.configure("hibernate.cfg.xml"); // đọc file cấu hình
                sessionFactory = config.buildSessionFactory();
                System.out.println("✅ Hibernate SessionFactory khởi tạo thành công!");
            } catch (Exception ex) {
                System.err.println("❌ Lỗi khởi tạo Hibernate: " + ex.getMessage());
                throw new ExceptionInInitializerError(ex);
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            System.out.println("Hibernate SessionFactory đã đóng.");
        }
    }
}
