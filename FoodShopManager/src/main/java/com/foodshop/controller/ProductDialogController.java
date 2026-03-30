package com.foodshop.controller;

import com.foodshop.config.HibernateUtil;
import com.foodshop.model.Category;
import com.foodshop.model.Product;
import com.foodshop.service.ProductService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class ProductDialogController implements Initializable {

    @FXML private TextField txtCode, txtName, txtPriceSell, txtPriceImport;
    @FXML private TextField txtStock, txtMinStock, txtOrigin, txtBrand;
    @FXML private ComboBox<Category> cbCategory;
    @FXML private ComboBox<String> cbUnit;
    @FXML private DatePicker dpExpiry;
    @FXML private TextArea txtDescription;
    @FXML private Label lblError;
    @FXML private Button btnSave;

    private Product product; // null = thêm mới
    private boolean saved = false;
    private final ProductService productService = ProductService.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Category> categories = session.createQuery(
                "FROM Category c WHERE c.active = true ORDER BY c.name", Category.class).list();
            cbCategory.setItems(FXCollections.observableArrayList(categories));
            cbCategory.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Category c) { return c == null ? "" : c.getName(); }
                @Override public Category fromString(String s) { return null; }
            });
        }

        cbUnit.setItems(FXCollections.observableArrayList(
            "kg", "gram", "hộp", "gói", "chai", "lon", "túi",
            "cái", "bó", "lít", "ml", "thùng", "vỉ", "tá"
        ));
    }

    public void setProduct(Product p) {
        this.product = p;
        if (p != null) {
            // Điền dữ liệu vào form
            txtCode.setText(p.getProductCode());
            txtCode.setDisable(true); // Không cho sửa mã
            txtName.setText(p.getName());
            txtPriceSell.setText(p.getPriceSell().toPlainString());
            if (p.getPriceImport() != null)
                txtPriceImport.setText(p.getPriceImport().toPlainString());
            cbCategory.setValue(p.getCategory());
            cbUnit.setValue(p.getUnit());
            txtStock.setText(String.valueOf(p.getQuantityInStock()));
            txtMinStock.setText(String.valueOf(p.getMinStockLevel()));
            if (p.getExpiryDate() != null) dpExpiry.setValue(p.getExpiryDate());
            if (p.getOrigin() != null) txtOrigin.setText(p.getOrigin());
            if (p.getBrand() != null) txtBrand.setText(p.getBrand());
            if (p.getDescription() != null) txtDescription.setText(p.getDescription());
            btnSave.setText("💾 Cập nhật");
        }
    }

    @FXML
    private void handleSave() {
        hideError();
        try {
            Product p = (product == null) ? new Product() : product;

            if (!txtCode.getText().isBlank()) p.setProductCode(txtCode.getText().trim());
            p.setName(txtName.getText().trim());
            p.setCategory(cbCategory.getValue());
            p.setUnit(cbUnit.getValue());
            p.setDescription(txtDescription.getText().trim());
            p.setOrigin(txtOrigin.getText().trim());
            p.setBrand(txtBrand.getText().trim());
            p.setExpiryDate(dpExpiry.getValue());

            String priceStr = txtPriceSell.getText().replaceAll("[^0-9.]", "");
            if (priceStr.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập giá bán!");
            p.setPriceSell(new BigDecimal(priceStr));

            String importStr = txtPriceImport.getText().replaceAll("[^0-9.]", "");
            if (!importStr.isEmpty()) p.setPriceImport(new BigDecimal(importStr));

            String stockStr = txtStock.getText().trim();
            p.setQuantityInStock(stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr));

            String minStr = txtMinStock.getText().trim();
            p.setMinStockLevel(minStr.isEmpty() ? 5 : Integer.parseInt(minStr));

            if (product == null) {
                productService.addProduct(p);
                showSuccess("Đã thêm sản phẩm: " + p.getName());
            } else {
                productService.updateProduct(p);
                showSuccess("Đã cập nhật sản phẩm: " + p.getName());
            }

            saved = true;
            closeDialog();

        } catch (NumberFormatException e) {
            showError("Giá tiền hoặc số lượng không hợp lệ!");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() { closeDialog(); }

    private void closeDialog() {
        ((Stage) btnSave.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        lblError.setText("⚠ " + msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void showSuccess(String msg) {
        MainController.showAlert(msg, Alert.AlertType.INFORMATION);
    }

    public boolean isSaved() { return saved; }
}
