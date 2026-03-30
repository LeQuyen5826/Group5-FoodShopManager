package com.foodshop.controller;

import com.foodshop.model.Category;
import com.foodshop.model.Product;
import com.foodshop.service.AuthService;
import com.foodshop.service.ProductService;
import com.foodshop.config.HibernateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProductController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<Category> cbCategoryFilter;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private TableView<Product> tblProducts;
    @FXML private TableColumn<Product, String> colCode;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Category> colCategory;
    @FXML private TableColumn<Product, String> colUnit;
    @FXML private TableColumn<Product, BigDecimal> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, LocalDate> colExpiry;
    @FXML private TableColumn<Product, Product.ProductStatus> colStatus;
    @FXML private TableColumn<Product, Void> colActions;
    @FXML private Label lblTotalCount;
    @FXML private Label lblLowStockCount;

    @FXML private Button btnAddProduct;
    @FXML private Button btnImportStock;

    private final ProductService productService = ProductService.getInstance();
    private final AuthService auth = AuthService.getInstance();
    private final NumberFormat currencyFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private ObservableList<Product> productList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadCategories();
        loadProducts();
        applyPermissions();
    }

    private void applyPermissions() {
        boolean canEdit = auth.canEditProducts();
        if (btnAddProduct != null) {
            btnAddProduct.setVisible(canEdit);
            btnAddProduct.setManaged(canEdit);
        }
        if (btnImportStock != null) {
            btnImportStock.setVisible(auth.canManageInventory());
            btnImportStock.setManaged(auth.canManageInventory());
        }
    }

    private void setupColumns() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCategory.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        colPrice.setCellValueFactory(new PropertyValueFactory<>("priceSell"));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFmt.format(item) + " ₫");
            }
        });

        colStock.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toString());
                if (item == 0) setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                else if (item <= 5) setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                else setStyle("-fx-text-fill: #27ae60;");
            }
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        colExpiry.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText("—"); setStyle(""); return; }
                setText(item.format(dtf));
                if (item.isBefore(LocalDate.now()))
                    setStyle("-fx-text-fill: #e74c3c;");
                else if (item.isBefore(LocalDate.now().plusDays(7)))
                    setStyle("-fx-text-fill: #f39c12;");
                else setStyle("");
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Product.ProductStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.getDisplayName());
                setStyle(switch (item) {
                    case AVAILABLE -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
                    case OUT_OF_STOCK -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
                    case DISCONTINUED -> "-fx-text-fill: #95a5a6;";
                    case EXPIRED -> "-fx-text-fill: #c0392b; -fx-font-weight: bold;";
                });
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnEdit   = new Button("✏ Sửa");
            final Button btnDelete = new Button("🗑 Xóa");
            final HBox box = new HBox(6, btnEdit, btnDelete);

            {
                btnEdit.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                btnDelete.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-text-fill: #e74c3c;");
                btnEdit.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    handleEditProduct(p);
                });
                btnDelete.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    handleDeleteProduct(p);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && !AuthService.getInstance().canEditProducts()) {
                    // STAFF chỉ được xem, không có nút sửa/xóa
                    setGraphic(null);
                } else {
                    setGraphic(empty ? null : box);
                }
            }
        });

        tblProducts.setItems(productList);
    }

    private void loadCategories() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Category> categories = session.createQuery(
                "FROM Category c WHERE c.active = true ORDER BY c.name", Category.class).list();

            cbCategoryFilter.getItems().clear();
            cbCategoryFilter.getItems().add(null); // "Tất cả"
            cbCategoryFilter.getItems().addAll(categories);
            cbCategoryFilter.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Category c) { return c == null ? "Tất cả danh mục" : c.getName(); }
                @Override public Category fromString(String s) { return null; }
            });
        }

        cbStatusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "Còn hàng", "Hết hàng", "Ngừng kinh doanh"
        ));
        cbStatusFilter.getSelectionModel().selectFirst();
    }

    @FXML
    public void loadProducts() {
        try {
            String keyword = txtSearch.getText();
            Category cat = cbCategoryFilter.getValue();
            String statusStr = cbStatusFilter.getValue();

            Product.ProductStatus status = null;
            if ("Còn hàng".equals(statusStr)) status = Product.ProductStatus.AVAILABLE;
            else if ("Hết hàng".equals(statusStr)) status = Product.ProductStatus.OUT_OF_STOCK;
            else if ("Ngừng kinh doanh".equals(statusStr)) status = Product.ProductStatus.DISCONTINUED;

            List<Product> products = productService.filterProducts(
                keyword, cat != null ? cat.getId() : null, status
            );

            productList.setAll(products);
            lblTotalCount.setText("Tổng: " + products.size() + " sản phẩm");

            long lowStock = products.stream().filter(Product::isLowStock).count();
            if (lowStock > 0) {
                lblLowStockCount.setText("⚠ " + lowStock + " sản phẩm sắp hết hàng");
                lblLowStockCount.setStyle("-fx-text-fill: #e67e22;");
            } else {
                lblLowStockCount.setText("");
            }

        } catch (Exception e) {
            MainController.showAlert("Lỗi tải danh sách sản phẩm: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML private void handleSearch() { loadProducts(); }
    @FXML private void handleFilter() { loadProducts(); }
    @FXML private void handleRefresh() {
        txtSearch.clear();
        cbCategoryFilter.getSelectionModel().clearSelection();
        cbStatusFilter.getSelectionModel().selectFirst();
        loadProducts();
    }

    @FXML
    private void handleAddProduct() {
        if (!auth.canEditProducts()) { denyAccess(); return; }
        openProductDialog(null);
    }

    private void denyAccess() {
        MainController.showAlert("🚫 Bạn không có quyền thực hiện thao tác này!", Alert.AlertType.WARNING);
    }

    private void handleEditProduct(Product product) {
        openProductDialog(product);
    }

    private void handleDeleteProduct(Product product) {
        if (!auth.canEditProducts()) { denyAccess(); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText("Xóa sản phẩm: " + product.getName() + "?");
        confirm.setContentText("Sản phẩm sẽ bị ngừng kinh doanh (dữ liệu đơn hàng cũ vẫn giữ nguyên).");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                productService.deactivateProduct(product.getId());
                loadProducts();
                MainController.showAlert("Đã ngừng kinh doanh sản phẩm: " + product.getName(),
                    Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                MainController.showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleImportStock() {
        if (!auth.canManageInventory()) { denyAccess(); return; }
        // Mở dialog nhập kho
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/foodshop/fxml/ImportStockDialog.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("📥 Nhập kho hàng");
            dialog.setScene(new Scene(root, 600, 450));
            dialog.showAndWait();
            loadProducts(); // Reload sau khi nhập
        } catch (Exception e) {
            MainController.showAlert("Chức năng nhập kho: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void openProductDialog(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/foodshop/fxml/ProductDialog.fxml"));
            Parent root = loader.load();

            ProductDialogController controller = loader.getController();
            controller.setProduct(product); // null = thêm mới

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(product == null ? "➕ Thêm sản phẩm mới" : "✏ Sửa sản phẩm");
            dialog.setScene(new Scene(root, 600, 580));
            dialog.showAndWait();

            if (controller.isSaved()) {
                loadProducts();
            }
        } catch (Exception e) {
            MainController.showAlert("Lỗi mở form sản phẩm: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}