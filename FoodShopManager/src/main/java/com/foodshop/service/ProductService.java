package com.foodshop.service;

import com.foodshop.model.*;
import com.foodshop.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ProductService {

    private final ProductRepository productRepo;
    private static ProductService instance;

    private ProductService() {
        this.productRepo = ProductRepository.getInstance();
    }

    public static ProductService getInstance() {
        if (instance == null) instance = new ProductService();
        return instance;
    }

    public Product addProduct(Product product) {
        validateProduct(product);

        if (product.getProductCode() == null || product.getProductCode().isBlank()) {
            product.setProductCode(productRepo.generateProductCode());
        } else if (productRepo.findByCode(product.getProductCode()).isPresent()) {
            throw new IllegalArgumentException("Mã sản phẩm '" + product.getProductCode() + "' đã tồn tại!");
        }

        return productRepo.save(product);
    }

    public Product updateProduct(Product product) {
        validateProduct(product);
        if (product.getId() == null) {
            throw new IllegalArgumentException("Không tìm thấy ID sản phẩm để cập nhật!");
        }
        return productRepo.update(product);
    }

    public void deactivateProduct(Long productId) {
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + productId));
        product.setStatus(Product.ProductStatus.DISCONTINUED);
        productRepo.update(product);
    }

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepo.findById(id);
    }

    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) return getAllProducts();
        return productRepo.searchByKeyword(keyword);
    }

    public List<Product> getProductsByCategory(Category category) {
        return productRepo.findByCategory(category);
    }

    public List<Product> getLowStockProducts() {
        return productRepo.findLowStockProducts();
    }

    public List<Product> getOutOfStockProducts() {
        return productRepo.findOutOfStockProducts();
    }

    public List<Product> filterProducts(String keyword, Long categoryId, Product.ProductStatus status) {
        return productRepo.findWithFilters(keyword, categoryId, status);
    }

    private void validateProduct(Product p) {
        if (p.getName() == null || p.getName().isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống!");
        }
        if (p.getCategory() == null) {
            throw new IllegalArgumentException("Phải chọn danh mục sản phẩm!");
        }
        if (p.getPriceSell() == null || p.getPriceSell().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá bán không hợp lệ!");
        }
        if (p.getUnit() == null || p.getUnit().isBlank()) {
            throw new IllegalArgumentException("Phải chọn đơn vị tính!");
        }
    }
}
