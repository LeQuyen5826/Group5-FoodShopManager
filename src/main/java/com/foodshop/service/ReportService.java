package com.foodshop.service;

import com.foodshop.config.HibernateUtil;
import com.foodshop.model.Order;
import com.foodshop.model.Product;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportService {

    private static ReportService instance;
    private ReportService() {}

    public static ReportService getInstance() {
        if (instance == null) instance = new ReportService();
        return instance;
    }

    public Map<String, BigDecimal> getRevenueByDay(LocalDateTime from, LocalDateTime to) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> rows = session.createNativeQuery(
                "SELECT DATE(order_date) as day, SUM(total_amount) as revenue " +
                "FROM orders WHERE status = 'COMPLETED' AND order_date BETWEEN :from AND :to " +
                "GROUP BY DATE(order_date) ORDER BY day", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .list();

            Map<String, BigDecimal> result = new LinkedHashMap<>();
            for (Object[] row : rows) {
                String day = row[0].toString().substring(5); // MM-dd
                BigDecimal revenue = new BigDecimal(row[1].toString());
                result.put(day, revenue);
            }
            return result;
        }
    }

    public Map<String, BigDecimal> getRevenueByMonth(int year) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> rows = session.createNativeQuery(
                "SELECT MONTH(order_date) as month, SUM(total_amount) as revenue " +
                "FROM orders WHERE status = 'COMPLETED' AND YEAR(order_date) = :yr " +
                "GROUP BY MONTH(order_date) ORDER BY month", Object[].class)
                .setParameter("yr", year).list();

            Map<String, BigDecimal> result = new LinkedHashMap<>();
            // Khởi tạo 12 tháng với 0
            for (int i = 1; i <= 12; i++) {
                result.put("T" + i, BigDecimal.ZERO);
            }
            for (Object[] row : rows) {
                int month = ((Number) row[0]).intValue();
                BigDecimal revenue = new BigDecimal(row[1].toString());
                result.put("T" + month, revenue);
            }
            return result;
        }
    }

    public List<Object[]> getTopSellingProducts(int limit, LocalDateTime from, LocalDateTime to) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createNativeQuery(
                "SELECT p.name, p.unit, SUM(od.quantity) as total_qty, SUM(od.line_total) as total_revenue " +
                "FROM order_details od " +
                "JOIN products p ON od.product_id = p.id " +
                "JOIN orders o ON od.order_id = o.id " +
                "WHERE o.status = 'COMPLETED' AND o.order_date BETWEEN :from AND :to " +
                "GROUP BY p.id, p.name, p.unit " +
                "ORDER BY total_qty DESC LIMIT :lim", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("lim", limit)
                .list();
        }
    }

    public Map<String, BigDecimal> getRevenueByCategory(LocalDateTime from, LocalDateTime to) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> rows = session.createNativeQuery(
                "SELECT c.name, SUM(od.line_total) as revenue " +
                "FROM order_details od " +
                "JOIN products p ON od.product_id = p.id " +
                "JOIN categories c ON p.category_id = c.id " +
                "JOIN orders o ON od.order_id = o.id " +
                "WHERE o.status = 'COMPLETED' AND o.order_date BETWEEN :from AND :to " +
                "GROUP BY c.id, c.name ORDER BY revenue DESC", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .list();

            Map<String, BigDecimal> result = new LinkedHashMap<>();
            for (Object[] row : rows) {
                result.put(row[0].toString(), new BigDecimal(row[1].toString()));
            }
            return result;
        }
    }

    public DashboardStats getDashboardStats() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            DashboardStats stats = new DashboardStats();

            Long todayOrders = session.createQuery(
                "SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :start", Long.class)
                .setParameter("start", startOfDay).uniqueResult();
            stats.todayOrders = todayOrders != null ? todayOrders : 0L;

            BigDecimal todayRev = session.createQuery(
                "SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.orderDate >= :start",
                BigDecimal.class).setParameter("start", startOfDay).uniqueResult();
            stats.todayRevenue = todayRev != null ? todayRev : BigDecimal.ZERO;

            BigDecimal monthRev = session.createQuery(
                "SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.orderDate >= :start",
                BigDecimal.class).setParameter("start", startOfMonth).uniqueResult();
            stats.monthRevenue = monthRev != null ? monthRev : BigDecimal.ZERO;

            Long totalProducts = session.createQuery(
                "SELECT COUNT(p) FROM Product p WHERE p.status != 'DISCONTINUED'", Long.class).uniqueResult();
            stats.totalProducts = totalProducts != null ? totalProducts : 0L;

            Long lowStock = session.createQuery(
                "SELECT COUNT(p) FROM Product p WHERE p.quantityInStock <= p.minStockLevel AND p.quantityInStock > 0",
                Long.class).uniqueResult();
            stats.lowStockProducts = lowStock != null ? lowStock : 0L;

            Long totalCustomers = session.createQuery("SELECT COUNT(c) FROM Customer c", Long.class).uniqueResult();
            stats.totalCustomers = totalCustomers != null ? totalCustomers : 0L;

            Long pendingOrders = session.createQuery(
                "SELECT COUNT(o) FROM Order o WHERE o.status IN ('PENDING', 'CONFIRMED', 'PROCESSING')",
                Long.class).uniqueResult();
            stats.pendingOrders = pendingOrders != null ? pendingOrders : 0L;

            return stats;
        }
    }

    public static class DashboardStats {
        public long todayOrders;
        public BigDecimal todayRevenue = BigDecimal.ZERO;
        public BigDecimal monthRevenue = BigDecimal.ZERO;
        public long totalProducts;
        public long lowStockProducts;
        public long totalCustomers;
        public long pendingOrders;
    }
}
