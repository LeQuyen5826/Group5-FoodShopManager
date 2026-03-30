  package com.foodshop.controller;

import com.foodshop.config.HibernateUtil;
import com.foodshop.service.AuthService;
import com.foodshop.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CustomerController implements Initializable {

    private final AuthService auth = AuthService.getInstance();

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbTypeFilter;
    @FXML private TableView<Customer> tblCustomers;
    @FXML private TableColumn<Customer, String> colName, colPhone, colEmail, colCity;
    @FXML private TableColumn<Customer, Customer.CustomerType> colType;
    @FXML private TableColumn<Customer, Integer> colPoints;
    @FXML private TableColumn<Customer, Long> colOrders;
    @FXML private TableColumn<Customer, Void> colActions;
    @FXML private Label lblTotal;

    private ObservableList<Customer> customerList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        cbTypeFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "Khách mới", "Khách thường", "Khách VIP", "Khách sỉ"));
        cbTypeFilter.getSelectionModel().selectFirst();
        loadCustomers();
    }

    private void setupColumns() {
        colName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getFullName()));
        colPhone.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getPhone()));
        colEmail.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getEmail() != null ? d.getValue().getEmail() : ""));
        colCity.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getCity() != null ? d.getValue().getCity() : ""));

        colType.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCustomerType()));
        colType.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Customer.CustomerType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toString());
                setStyle(switch (item) {
                    case VIP       -> "-fx-text-fill: #7c3aed; -fx-font-weight:bold;";
                    case WHOLESALE -> "-fx-text-fill: #0369a1; -fx-font-weight:bold;";
                    case NEW       -> "-fx-text-fill: #16a34a;";
                    default        -> "-fx-text-fill: #374151;";
                });
            }
        });

        colPoints.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(
            d.getValue().getLoyaltyPoints()).asObject());

        colOrders.setCellValueFactory(d -> {
            long count = d.getValue().getOrders().size();
            return new javafx.beans.property.SimpleLongProperty(count).asObject();
        });

        colActions.setCellFactory(c -> new TableCell<>() {
            final Button btnEdit = new Button("✏ Sửa");
            final Button btnDel  = new Button("🗑");
            final HBox box = new HBox(6, btnEdit, btnDel);
            {
                btnEdit.setStyle("-fx-font-size:11px;-fx-padding:3 8;");
                btnDel.setStyle("-fx-font-size:11px;-fx-padding:3 6;-fx-text-fill:#e74c3c;");
                btnEdit.setOnAction(e -> openDialog(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : box);
            }
        });

        tblCustomers.setItems(customerList);
    }

    @FXML
    public void loadCustomers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String kw = txtSearch.getText().toLowerCase().trim();
            String typeStr = cbTypeFilter.getValue();

            // LEFT JOIN FETCH để load orders trong cùng session, tránh LazyInitializationException
            String hql = "SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.orders WHERE 1=1";
            if (!kw.isEmpty()) hql += " AND (LOWER(c.fullName) LIKE :kw OR c.phone LIKE :kw)";
            if (typeStr != null && !typeStr.equals("Tất cả")) {
                Customer.CustomerType type = switch (typeStr) {
                    case "Khách mới" -> Customer.CustomerType.NEW;
                    case "Khách VIP" -> Customer.CustomerType.VIP;
                    case "Khách sỉ"  -> Customer.CustomerType.WHOLESALE;
                    default           -> Customer.CustomerType.REGULAR;
                };
                hql += " AND c.customerType = :type";
                var q = session.createQuery(hql, Customer.class);
                if (!kw.isEmpty()) q.setParameter("kw", "%" + kw + "%");
                q.setParameter("type", type);
                customerList.setAll(q.list());
            } else {
                var q = session.createQuery(hql + " ORDER BY c.fullName", Customer.class);
                if (!kw.isEmpty()) q.setParameter("kw", "%" + kw + "%");
                customerList.setAll(q.list());
            }
            lblTotal.setText("Tổng: " + customerList.size() + " khách hàng");
        } catch (Exception e) {
            MainController.showAlert("Lỗi tải danh sách: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML private void handleSearch() { loadCustomers(); }
    @FXML private void handleFilter() { loadCustomers(); }
    @FXML private void handleRefresh() {
        txtSearch.clear();
        cbTypeFilter.getSelectionModel().selectFirst();
        loadCustomers();
    }

    @FXML
    private void handleAdd() {
        if (!auth.canManageCustomers()) { MainController.showAlert("🚫 Bạn không có quyền thêm khách hàng!", Alert.AlertType.WARNING); return; } openDialog(null); }

    private void openDialog(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/foodshop/fxml/CustomerDialog.fxml"));
            Parent root = loader.load();
            CustomerDialogController ctrl = loader.getController();
            ctrl.setCustomer(customer);
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(customer == null ? "➕ Thêm khách hàng" : "✏ Sửa khách hàng");
            dialog.setScene(new Scene(root, 520, 480));
            dialog.showAndWait();
            if (ctrl.isSaved()) loadCustomers();
        } catch (Exception e) {
            MainController.showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleDelete(Customer customer) {
        if (!customer.getOrders().isEmpty()) {
            MainController.showAlert(
                "Không thể xóa! Khách hàng '" + customer.getFullName() + "' đã có " +
                customer.getOrders().size() + " đơn hàng.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Xóa khách hàng: " + customer.getFullName() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Transaction tx = null;
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                tx = session.beginTransaction();
                session.remove(session.merge(customer));
                tx.commit();
                loadCustomers();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                MainController.showAlert("Lỗi xóa: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
}
