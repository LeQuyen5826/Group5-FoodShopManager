package com.foodshop.repository;

import com.foodshop.model.Customer;
import com.foodshop.model.Order;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class OrderRepository extends BaseRepository<Order, Long> {

    private static OrderRepository instance;

    private OrderRepository() { super(Order.class); }

    public static OrderRepository getInstance() {
        if (instance == null) instance = new OrderRepository();
        return instance;
    }
    
    public Optional<Order> findByCode(String code) {
        try (Session session = getSession()) {
            return session.createQuery("FROM Order o WHERE o.orderCode = :code", Order.class)
                .setParameter("code", code).uniqueResultOptional();
        }
    }

    public List<Order> findByCustomer(Customer customer) {
        try (Session session = getSession()) {
            return session.createQuery(
                "FROM Order o WHERE o.customer = :c ORDER BY o.orderDate DESC", Order.class)
                .setParameter("c", customer).list();
        }
    }

    public List<Order> findByDateRange(LocalDateTime from, LocalDateTime to) {
        try (Session session = getSession()) {
            return session.createQuery(
                "FROM Order o WHERE o.orderDate BETWEEN :from AND :to ORDER BY o.orderDate DESC",
                Order.class)
                .setParameter("from", from)
                .setParameter("to", to).list();
        }
    }

    public List<Order> findByStatus(Order.OrderStatus status) {
        try (Session session = getSession()) {
            return session.createQuery(
                "FROM Order o WHERE o.status = :status ORDER BY o.orderDate DESC", Order.class)
                .setParameter("status", status).list();
        }
    }

    public BigDecimal getTotalRevenueByDateRange(LocalDateTime from, LocalDateTime to) {
        try (Session session = getSession()) {
            BigDecimal result = session.createQuery(
                "SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' " +
                "AND o.orderDate BETWEEN :from AND :to", BigDecimal.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .uniqueResult();
            return result != null ? result : BigDecimal.ZERO;
        }
    }

    public long countTodayOrders() {
        try (Session session = getSession()) {
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            Long count = session.createQuery(
                "SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :start", Long.class)
                .setParameter("start", startOfDay).uniqueResult();
            return count != null ? count : 0L;
        }
    }

    public String generateOrderCode() {
        int year = java.time.LocalDate.now().getYear();
        try (Session session = getSession()) {
            Long count = session.createQuery(
                "SELECT COUNT(o) FROM Order o WHERE YEAR(o.orderDate) = :yr", Long.class)
                .setParameter("yr", year).uniqueResult();
            return String.format("DH%d%03d", year, (count != null ? count : 0) + 1);
        }
    }
}
