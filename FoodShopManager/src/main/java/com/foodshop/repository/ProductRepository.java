package com.foodshop.repository;

import com.foodshop.model.Category;
import com.foodshop.model.Product;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.List;
import java.util.Optional;

public class ProductRepository extends BaseRepository<Product, Long> {

    private static ProductRepository instance;

    private ProductRepository() {
        super(Product.class);
    }

    public static ProductRepository getInstance() {
        if (instance == null) instance = new ProductRepository();
        return instance;
    }

    public Optional<Product> findByCode(String code) {
        try (Session session = getSession()) {
            Query<Product> q = session.createQuery(
                "FROM Product p WHERE p.productCode = :code", Product.class);
            q.setParameter("code", code);
            return q.uniqueResultOptional();
        }
    }

    public List<Product> searchByKeyword(String keyword) {
        try (Session session = getSession()) {
            String hql = "FROM Product p WHERE LOWER(p.name) LIKE :kw OR LOWER(p.productCode) LIKE :kw";
            Query<Product> q = session.createQuery(hql, Product.class);
            q.setParameter("kw", "%" + keyword.toLowerCase() + "%");
            return q.list();
        }
    }

    public List<Product> findByCategory(Category category) {
        try (Session session = getSession()) {
            Query<Product> q = session.createQuery(
                "FROM Product p WHERE p.category = :cat ORDER BY p.name", Product.class);
            q.setParameter("cat", category);
            return q.list();
        }
    }

    public List<Product> findLowStockProducts() {
        try (Session session = getSession()) {
            String hql = "FROM Product p WHERE p.quantityInStock <= p.minStockLevel AND p.quantityInStock > 0";
            return session.createQuery(hql, Product.class).list();
        }
    }

    public List<Product> findOutOfStockProducts() {
        try (Session session = getSession()) {
            String hql = "FROM Product p WHERE p.quantityInStock = 0 AND p.status != 'DISCONTINUED'";
            return session.createQuery(hql, Product.class).list();
        }
    }

    public List<Product> findWithFilters(String keyword, Long categoryId, Product.ProductStatus status) {
        try (Session session = getSession()) {
            StringBuilder hql = new StringBuilder("FROM Product p WHERE 1=1");
            if (keyword != null && !keyword.isBlank()) {
                hql.append(" AND (LOWER(p.name) LIKE :kw OR LOWER(p.productCode) LIKE :kw)");
            }
            if (categoryId != null) {
                hql.append(" AND p.category.id = :catId");
            }
            if (status != null) {
                hql.append(" AND p.status = :status");
            }
            hql.append(" ORDER BY p.name");

            Query<Product> q = session.createQuery(hql.toString(), Product.class);

            if (keyword != null && !keyword.isBlank()) {
                q.setParameter("kw", "%" + keyword.toLowerCase() + "%");
            }
            if (categoryId != null) q.setParameter("catId", categoryId);
            if (status != null) q.setParameter("status", status);

            return q.list();
        }
    }

    public String generateProductCode() {
        try (Session session = getSession()) {
            Long count = session.createQuery("SELECT COUNT(p) FROM Product p", Long.class)
                .uniqueResult();
            return String.format("SP%03d", count + 1);
        }
    }

    public void updateStock(Long productId, int newQuantity) {
        org.hibernate.Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                "UPDATE Product p SET p.quantityInStock = :qty WHERE p.id = :id")
                .setParameter("qty", newQuantity)
                .setParameter("id", productId)
                .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Lỗi cập nhật tồn kho: " + e.getMessage(), e);
        }
    }
}
