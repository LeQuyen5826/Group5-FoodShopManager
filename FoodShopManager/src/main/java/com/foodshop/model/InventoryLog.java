package com.foodshop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange; 

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost; 

    @Column(name = "reference_code", length = 50)
    private String referenceCode; 

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ActionType {
        IMPORT("Nhập kho"),
        EXPORT_SALE("Xuất bán"),
        EXPORT_RETURN("Xuất trả"),
        ADJUSTMENT("Điều chỉnh"),
        DAMAGE("Hàng hỏng");

        private final String displayName;
        ActionType(String displayName) { this.displayName = displayName; }

        @Override
        public String toString() { return displayName; }
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public InventoryLog() {}

    public InventoryLog(Product product, User user, ActionType actionType,
                        int quantityChange, int quantityBefore) {
        this.product = product;
        this.user = user;
        this.actionType = actionType;
        this.quantityChange = quantityChange;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityBefore + quantityChange;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }
    public Integer getQuantityBefore() { return quantityBefore; }
    public void setQuantityBefore(Integer quantityBefore) { this.quantityBefore = quantityBefore; }
    public Integer getQuantityAfter() { return quantityAfter; }
    public void setQuantityAfter(Integer quantityAfter) { this.quantityAfter = quantityAfter; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
