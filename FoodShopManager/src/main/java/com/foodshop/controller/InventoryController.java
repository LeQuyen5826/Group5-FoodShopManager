package com.foodshop.controller;

import com.foodshop.config.HibernateUtil;
import com.foodshop.model.*;
import com.foodshop.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class InventoryController implements Initializable {

    @FXML private ComboBox<Product> cbImportProduct;
    @FXML private TextField txtImportQty, txtImportCost;
    @FXML private TextArea txtImportNote;
    @FXML private Button btnAddProduct;

    @FXML private DatePicker dpLogFrom, dpLogTo;
    @FXML private ComboBox<String> cbLogType;
    @FXML private TableView<InventoryLog> tblLogs;
    @FXML private TableColumn<InventoryLog, String> logColDate, logColProduct, logColType, logColRef, logColUser;
    @FXML private TableColumn<InventoryLog, Integer> logColChange, logColBefore, logColAfter;

    @FXML private TableView<Product> tblStock;
    @FXML private TableColumn<Product, String> sColCode, sColName, sColCategory, sColStatus;
    @FXML private TableColumn<Product, Integer> sColStock, sColMin;
    @FXML private TableColumn<Product, LocalDate> sColExpiry;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadProducts();
        setupLogTable();
        setupStockTable();
        cbLogType.setItems(FXCollections.observableArrayList(
            "Tất cả", "Nhập kho", "Xuất bán", "Điều chỉnh", "Hàng hỏng"));
        cbLogType.getSelectionModel().selectFirst();
        dpLogFrom.setValue(LocalDate.now().minusDays(30));
        dpLogTo.setValue(LocalDate.now());
        loadLogs();
        loadStockSummary();

        if (btnAddProduct != null) {
            boolean canEdit = AuthService.getInstance().canEditProducts();
            btnAddProduct.setVisible(canEdit);
            btnAddProduct.setManaged(canEdit);
        }
    }

    private void loadProducts() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Product> products = session.createQuery(
                "FROM Product p WHERE p.status != 'DISCONTINUED' ORDER BY p.name", Product.class).list();
            cbImportProduct.setItems(FXCollections.observableArrayList(products));
            cbImportProduct.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Product p) { return p == null ? "" : "[" + p.getProductCode() + "] " + p.getName(); }
                @Override public Product fromString(String s) { return null; }
            });
        }
    }

    @FXML
    private void handleImport() {
        if (!AuthService.getInstance().canManageInventory()) {
            MainController.showAlert("🚫 Bạn không có quyền nhập kho!", Alert.AlertType.WARNING);
            return;
        }
        Product product = cbImportProduct.getValue();
        if (product == null) { MainController.showAlert("Vui lòng chọn sản phẩm!", Alert.AlertType.WARNING); return; }

        int qty;
        try { qty = Integer.parseInt(txtImportQty.getText().trim()); }
        catch (NumberFormatException e) { MainController.showAlert("Số lượng không hợp lệ!", Alert.AlertType.WARNING); return; }
        if (qty <= 0) { MainController.showAlert("Số lượng phải > 0!", Alert.AlertType.WARNING); return; }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Product p = session.get(Product.class, product.getId());
            int before = p.getQuantityInStock();
            p.setQuantityInStock(before + qty);
            session.merge(p);

            User currentUser = AuthService.getInstance().getCurrentUser();
            User managedUser = currentUser != null ? session.merge(currentUser) : null;
            InventoryLog log = new InventoryLog(p, managedUser,
                InventoryLog.ActionType.IMPORT, qty, before);
            log.setNotes(txtImportNote.getText().trim());
            String costStr = txtImportCost.getText().replaceAll("[^0-9.]", "");
            if (!costStr.isEmpty()) log.setUnitCost(new BigDecimal(costStr));
            session.persist(log);
            tx.commit();

            MainController.showAlert("✅ Nhập kho thành công!\n" + p.getName() +
                " — Thêm " + qty + " " + p.getUnit() + " | Tổng tồn: " + (before + qty),
                Alert.AlertType.INFORMATION);
            txtImportQty.clear(); txtImportCost.clear(); txtImportNote.clear();
            cbImportProduct.setValue(null);
            loadProducts(); loadLogs(); loadStockSummary();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            MainController.showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/foodshop/fxml/ProductDialog.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("➕ Thêm sản phẩm mới");
            dialog.setScene(new Scene(root, 600, 520));
            dialog.showAndWait();
            loadProducts(); 
        } catch (Exception e) {
            MainController.showAlert("Lỗi mở form thêm sản phẩm: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupLogTable() {
        logColDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(dtf) : ""));
        logColProduct.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getProduct().getName()));
        logColType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getActionType().toString()));
        logColType.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.contains("Nhập") ? "-fx-text-fill:#16a34a;-fx-font-weight:bold;" :
                         item.contains("Xuất") ? "-fx-text-fill:#dc2626;-fx-font-weight:bold;" :
                         "-fx-text-fill:#6b7280;");
            }
        });
        logColChange.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(
            d.getValue().getQuantityChange()).asObject());
        logColChange.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText((v > 0 ? "+" : "") + v);
                setStyle(v > 0 ? "-fx-text-fill:#16a34a;-fx-font-weight:bold;" : "-fx-text-fill:#dc2626;-fx-font-weight:bold;");
            }
        });
        logColBefore.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getQuantityBefore()).asObject());
        logColAfter.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getQuantityAfter()).asObject());
        logColRef.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getReferenceCode() != null ? d.getValue().getReferenceCode() : ""));
        logColUser.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getUser() != null ? d.getValue().getUser().getFullName() : ""));
    }

    @FXML
    public void loadLogs() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDateTime from = dpLogFrom.getValue() != null ?
                dpLogFrom.getValue().atStartOfDay() : LocalDateTime.now().minusDays(30).withHour(0);
            LocalDateTime to = dpLogTo.getValue() != null ?
                dpLogTo.getValue().atTime(23,59,59) : LocalDateTime.now();

            List<InventoryLog> logs = session.createQuery(
                "FROM InventoryLog l WHERE l.createdAt BETWEEN :from AND :to ORDER BY l.createdAt DESC",
                InventoryLog.class)
                .setParameter("from", from).setParameter("to", to).list();

            tblLogs.setItems(FXCollections.observableArrayList(logs));
        } catch (Exception e) {
            MainController.showAlert("Lỗi tải lịch sử: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupStockTable() {
        sColCode.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getProductCode()));
        sColName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
        sColCategory.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getCategory() != null ? d.getValue().getCategory().getName() : ""));
        sColStock.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getQuantityInStock()).asObject());
        sColStock.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v.toString());
                setStyle(v == 0 ? "-fx-text-fill:#dc2626;-fx-font-weight:bold;" :
                         v <= 5 ? "-fx-text-fill:#f97316;-fx-font-weight:bold;" :
                         "-fx-text-fill:#16a34a;");
            }
        });
        sColMin.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getMinStockLevel()).asObject());
        sColStatus.setCellValueFactory(d -> {
            Product p = d.getValue();
            String s = p.getQuantityInStock() == 0 ? "🔴 Hết hàng" :
                       p.isLowStock() ? "🟡 Sắp hết" : "🟢 Ổn định";
            return new javafx.beans.property.SimpleStringProperty(s);
        });
        sColExpiry.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getExpiryDate()));
        sColExpiry.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText("—"); setStyle(""); return; }
                setText(v.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                setStyle(v.isBefore(LocalDate.now()) ? "-fx-text-fill:#dc2626;-fx-font-weight:bold;" :
                         v.isBefore(LocalDate.now().plusDays(7)) ? "-fx-text-fill:#f97316;" : "");
            }
        });
    }

    @FXML
    public void loadStockSummary() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Product> products = session.createQuery(
                "FROM Product p WHERE p.status != 'DISCONTINUED' ORDER BY p.quantityInStock ASC", Product.class).list();
            tblStock.setItems(FXCollections.observableArrayList(products));
        }
    }

    @FXML
    public void showLowStock() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Product> products = session.createQuery(
                "FROM Product p WHERE p.quantityInStock <= p.minStockLevel AND p.status != 'DISCONTINUED'",
                Product.class).list();
            tblStock.setItems(FXCollections.observableArrayList(products));
        }
    }
}
