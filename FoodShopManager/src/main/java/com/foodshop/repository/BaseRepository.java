package com.foodshop.repository;

import com.foodshop.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
import java.util.Optional;

public abstract class BaseRepository<T, ID> {

    protected final Class<T> entityClass;

    protected BaseRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected Session getSession() {
        return HibernateUtil.getSessionFactory().openSession();
    }

    public T save(T entity) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            session.persist(entity);
            tx.commit();
            return entity;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Lỗi khi lưu dữ liệu: " + e.getMessage(), e);
        }
    }

    public T update(T entity) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            T merged = session.merge(entity);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Lỗi khi cập nhật dữ liệu: " + e.getMessage(), e);
        }
    }

    public void delete(T entity) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            T merged = session.merge(entity);
            session.remove(merged);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Lỗi khi xóa dữ liệu: " + e.getMessage(), e);
        }
    }

    public void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }

    public Optional<T> findById(ID id) {
        try (Session session = getSession()) {
            T entity = session.get(entityClass, (java.io.Serializable) id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo ID: " + e.getMessage(), e);
        }
    }

    public List<T> findAll() {
        try (Session session = getSession()) {
            String hql = "FROM " + entityClass.getSimpleName();
            return session.createQuery(hql, entityClass).list();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách: " + e.getMessage(), e);
        }
    }
    
    public long count() {
        try (Session session = getSession()) {
            String hql = "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e";
            return session.createQuery(hql, Long.class).uniqueResult();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi đếm bản ghi: " + e.getMessage(), e);
        }
    }
}
