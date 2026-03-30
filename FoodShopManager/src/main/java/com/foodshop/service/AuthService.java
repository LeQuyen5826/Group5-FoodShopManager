package com.foodshop.service;

import com.foodshop.model.User;
import com.foodshop.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.Optional;

public class AuthService {

    private static AuthService instance;
    private User currentUser;

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }


    public User login(String username, String password) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập tên đăng nhập!");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu!");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Optional<User> userOpt = session.createQuery(
                "FROM User u WHERE u.username = :un AND u.active = true", User.class)
                .setParameter("un", username.trim())
                .uniqueResultOptional();

            if (userOpt.isEmpty())
                throw new IllegalArgumentException("Tài khoản không tồn tại hoặc đã bị khóa!");

            User user = userOpt.get();
            if (!BCrypt.checkpw(password, user.getPasswordHash()))
                throw new IllegalArgumentException("Mật khẩu không đúng!");

            Transaction tx = session.beginTransaction();
            user.setLastLogin(LocalDateTime.now());
            session.merge(user);
            tx.commit();

            this.currentUser = user;
            System.out.println("✅ Đăng nhập: " + user.getFullName() + " [" + user.getRole() + "]");
            return user;
        }
    }

    public void logout() {
        System.out.println("Đăng xuất: " + (currentUser != null ? currentUser.getUsername() : "?"));
        this.currentUser = null;
    }

    public User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn()  { return currentUser != null; }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == User.Role.ADMIN;
    }

    public boolean isStaff() {
        return currentUser != null && currentUser.getRole() == User.Role.STAFF;
    }

    public boolean isWarehouse() {
        return currentUser != null && currentUser.getRole() == User.Role.WAREHOUSE;
    }

    public boolean canManageOrders() {
        return isAdmin() || isStaff();
    }

    public boolean canManageCustomers() {
        return isAdmin() || isStaff();
    }

    public boolean canManageInventory() {
        return isAdmin() || isWarehouse();
    }

    public boolean canViewReports() {
        return isAdmin();
    }

    public boolean canEditProducts() {
        return isAdmin() || isWarehouse();
    }

    public boolean canManageUsers() {
        return isAdmin();
    }


    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public void createDefaultAdminIfNotExists() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("SELECT COUNT(u) FROM User u", Long.class).uniqueResult();
            if (count == null || count == 0) {
                Transaction tx = session.beginTransaction();
                User admin = new User("admin", hashPassword("admin123"),
                    "Quản trị viên", User.Role.ADMIN);
                admin.setEmail("admin@foodshop.com");
                session.persist(admin);
                tx.commit();
                System.out.println("✅ Tạo tài khoản Admin mặc định: admin / admin123");
            }
        }
    }
}
