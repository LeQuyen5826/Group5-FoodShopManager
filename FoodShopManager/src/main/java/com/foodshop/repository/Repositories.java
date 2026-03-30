package com.foodshop.repository;

import com.foodshop.model.Category;
import com.foodshop.model.Customer;
import com.foodshop.model.User;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

class CustomerRepository extends BaseRepository<Customer, Long> {

    private static CustomerRepository instance;
    private CustomerRepository() { super(Customer.class); }
    public static CustomerRepository getInstance() {
        if (instance == null) instance = new CustomerRepository();
        return instance;
    }

    public Optional<Customer> findByPhone(String phone) {
        try (Session session = getSession()) {
            return session.createQuery("FROM Customer c WHERE c.phone = :p", Customer.class)
                .setParameter("p", phone).uniqueResultOptional();
        }
    }

    public List<Customer> searchByKeyword(String keyword) {
        try (Session session = getSession()) {
            String hql = "FROM Customer c WHERE LOWER(c.fullName) LIKE :kw OR c.phone LIKE :kw";
            return session.createQuery(hql, Customer.class)
                .setParameter("kw", "%" + keyword.toLowerCase() + "%").list();
        }
    }

    public List<Customer> findByType(Customer.CustomerType type) {
        try (Session session = getSession()) {
            return session.createQuery("FROM Customer c WHERE c.customerType = :t", Customer.class)
                .setParameter("t", type).list();
        }
    }
}

class CategoryRepository extends BaseRepository<Category, Long> {

    private static CategoryRepository instance;
    private CategoryRepository() { super(Category.class); }
    public static CategoryRepository getInstance() {
        if (instance == null) instance = new CategoryRepository();
        return instance;
    }

    public List<Category> findAllActive() {
        try (Session session = getSession()) {
            return session.createQuery("FROM Category c WHERE c.active = true ORDER BY c.name", Category.class).list();
        }
    }

    public Optional<Category> findByName(String name) {
        try (Session session = getSession()) {
            return session.createQuery("FROM Category c WHERE c.name = :n", Category.class)
                .setParameter("n", name).uniqueResultOptional();
        }
    }
}

class UserRepository extends BaseRepository<User, Long> {

    private static UserRepository instance;
    private UserRepository() { super(User.class); }
    public static UserRepository getInstance() {
        if (instance == null) instance = new UserRepository();
        return instance;
    }

    public Optional<User> findByUsername(String username) {
        try (Session session = getSession()) {
            return session.createQuery("FROM User u WHERE u.username = :un AND u.active = true", User.class)
                .setParameter("un", username).uniqueResultOptional();
        }
    }

    public List<User> findAllActive() {
        try (Session session = getSession()) {
            return session.createQuery("FROM User u WHERE u.active = true ORDER BY u.fullName", User.class).list();
        }
    }
}
