package com.foodshop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_details")
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice; 

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "notes", length = 200)
    private String notes;

    @PrePersist
    @PreUpdate
    public void calculateLineTotal() {
        BigDecimal baseTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountFactor = BigDecimal.ONE.subtract(
                discountPercent.divide(BigDecimal.valueOf(100))
            );
            lineTotal = baseTotal.multiply(discountFactor);
        } else {
            lineTotal = baseTotal;
        }
    }

    public OrderDetail() {}

    public OrderDetail(Product product, Integer quantity, BigDecimal unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        calculateLineTotal();
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
