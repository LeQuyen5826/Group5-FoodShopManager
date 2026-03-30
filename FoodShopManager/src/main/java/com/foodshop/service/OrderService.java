package com.foodshop.service;

import com.foodshop.model.*;
import com.foodshop.repository.OrderRepository;
import com.foodshop.repository.ProductRepository;
import com.foodshop.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private static OrderService instance;

    private OrderService() {
        this.orderRepo = OrderRepository.getInstance();
        this.productRepo = ProductRepository.getInstance();
    }

    public static OrderService getInstance() {
        if (instance == null) instance = new OrderService();
        return instance;
    }

    public Order createOrder(Order order, User currentUser) {
        // Validate trước khi lưu
        validateOrder(order);

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            if (order.getOrderCode() == null || order.getOrderCode().isBlank()) {
                order.setOrderCode(orderRepo.generateOrderCode());
            }
            order.setCreatedBy(currentUser);
            order.setOrderDate(LocalDateTime.now());
            order.recalculateTotal();

            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = session.get(Product.class, detail.getProduct().getId());

                if (product == null) {
                    throw new IllegalStateException("Sản phẩm không tồn tại: " + detail.getProduct().getName());
                }
                if (product.getQuantityInStock() < detail.getQuantity()) {
                    throw new IllegalStateException(
                        "Sản phẩm '" + product.getName() + "' không đủ tồn kho! " +
                        "Còn: " + product.getQuantityInStock() + " | Yêu cầu: " + detail.getQuantity()
                    );
                }

                int stockBefore = product.getQuantityInStock();
                product.setQuantityInStock(stockBefore - detail.getQuantity());
                session.merge(product);

                InventoryLog log = new InventoryLog(product, currentUser,
                    InventoryLog.ActionType.EXPORT_SALE, -detail.getQuantity(), stockBefore);
                log.setReferenceCode(order.getOrderCode());
                session.persist(log);
            }

            session.persist(order);
            tx.commit();

            System.out.println("✅ Tạo đơn hàng thành công: " + order.getOrderCode());
            return order;

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Lỗi tạo đơn hàng: " + e.getMessage(), e);
        }
    }

    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus, User currentUser) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng!"));

        Order.OrderStatus oldStatus = order.getStatus();

        if (newStatus == Order.OrderStatus.CANCELLED && oldStatus != Order.OrderStatus.CANCELLED) {
            restoreStock(order, currentUser);
        }

        if (newStatus == Order.OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        order.setStatus(newStatus);
        return orderRepo.update(order);
    }

    private void restoreStock(Order order, User currentUser) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = session.get(Product.class, detail.getProduct().getId());
                if (product != null) {
                    int stockBefore = product.getQuantityInStock();
                    product.setQuantityInStock(stockBefore + detail.getQuantity());
                    session.merge(product);

                    InventoryLog log = new InventoryLog(product, currentUser,
                        InventoryLog.ActionType.EXPORT_RETURN, detail.getQuantity(), stockBefore);
                    log.setReferenceCode(order.getOrderCode() + "-HUY");
                    session.persist(log);
                }
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Lỗi hoàn kho: " + e.getMessage(), e);
        }
    }

    public List<Order> getAllOrders() { return orderRepo.findAll(); }

    public Optional<Order> getOrderById(Long id) { return orderRepo.findById(id); }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepo.findByStatus(status);
    }

    public List<Order> getOrdersByDateRange(LocalDateTime from, LocalDateTime to) {
        return orderRepo.findByDateRange(from, to);
    }

    public BigDecimal getTodayRevenue() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = LocalDateTime.now();
        return orderRepo.getTotalRevenueByDateRange(start, end);
    }

    public long getTodayOrderCount() { return orderRepo.countTodayOrders(); }

    private void validateOrder(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải có ít nhất 1 sản phẩm!");
        }
        for (OrderDetail d : order.getOrderDetails()) {
            if (d.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng sản phẩm phải > 0!");
            }
        }
    }
}
