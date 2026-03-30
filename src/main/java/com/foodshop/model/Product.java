package com.foodshop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_code", columnList = "product_code"),
    @Index(name = "idx_product_name", columnList = "name")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true, length = 20)
    private String productCode; // SP001, SP002...

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "price_import", precision = 15, scale = 2)
    private BigDecimal priceImport; 

    @Column(name = "price_sell", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceSell;

    @Column(name = "quantity_in_stock", nullable = false)
    private Integer quantityInStock = 0;

    @Column(name = "min_stock_level")
    private Integer minStockLevel = 5; 

    @Column(name = "expiry_date")
    private LocalDate expiryDate; 

    @Column(name = "origin", length = 100)
    private String origin;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "weight_per_unit", precision = 10, scale = 3)
    private BigDecimal weightPerUnit;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status = ProductStatus.AVAILABLE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    public enum ProductStatus {
        AVAILABLE("Còn hàng"),
        OUT_OF_STOCK("Hết hàng"),
        DISCONTINUED("Ngừng kinh doanh"),
        EXPIRED("Hết hạn");

        private final String displayName;
        ProductStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }

        @Override
        public String toString() { return displayName; }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (quantityInStock <= 0) {
            status = ProductStatus.OUT_OF_STOCK;
        } else if (status == ProductStatus.OUT_OF_STOCK) {
            status = ProductStatus.AVAILABLE;
        }
    }

    public boolean isLowStock() {
        return quantityInStock <= minStockLevel && quantityInStock > 0;
    }

    public boolean isNearExpiry() {
        if (expiryDate == null) return false;
        return expiryDate.isBefore(LocalDate.now().plusDays(7));
    }

    public BigDecimal getProfitPerUnit() {
        if (priceImport == null) return BigDecimal.ZERO;
        return priceSell.subtract(priceImport);
    }

    public Product() {}

    public Product(String productCode, String name, Category category,
                   String unit, BigDecimal priceSell) {
        this.productCode = productCode;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.priceSell = priceSell;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getPriceImport() { return priceImport; }
    public void setPriceImport(BigDecimal priceImport) { this.priceImport = priceImport; }

    public BigDecimal getPriceSell() { return priceSell; }
    public void setPriceSell(BigDecimal priceSell) { this.priceSell = priceSell; }

    public Integer getQuantityInStock() { return quantityInStock; }
    public void setQuantityInStock(Integer quantityInStock) { this.quantityInStock = quantityInStock; }

    public Integer getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(Integer minStockLevel) { this.minStockLevel = minStockLevel; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public BigDecimal getWeightPerUnit() { return weightPerUnit; }
    public void setWeightPerUnit(BigDecimal weightPerUnit) { this.weightPerUnit = weightPerUnit; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() { return "[" + productCode + "] " + name; }
}
