package com.foodshop.controller;

import com.foodshop.model.Order;
import com.foodshop.service.AuthService;
import com.foodshop.service.OrderService;
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

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class OrderController implements Initializable {

    private final AuthService auth = AuthService.getInstance();

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private DatePicker dpFrom, dpTo;
    @FXML private TableView<Order> tblOrders;
    @FXML private TableColumn<Order, String> colCode, colCustomer, colPayment, colCreatedBy, colDate;
    @FXML private TableColumn<Order, Integer> colItems;
    @FXML private TableColumn<Order, BigDecimal> colTotal;
    @FXML private TableColumn<Order, Order.OrderStatus> colStatus;
    @FXML private TableColumn<Order, Void> colActions;
    @FXML private Label lblTotal, lblPending, lblTotalRevenue;

    private final OrderService orderService = OrderService.getInstance();
    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private ObservableList<Order> orderList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        cbStatusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đang xử lý",
            "Đang giao", "Hoàn thành", "Đã hủy"
        ));
        cbStatusFilter.getSelectionModel().selectFirst();
        loadOrders();
    }

    private void setupColumns() {
        colCode.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getOrderCode()));

        colCustomer.setCellValueFactory(d -> {
            Order o = d.getValue();
            String name = o.getCustomer() != null ? o.getCustomer().getFullName() : "Khách lẻ";
            String phone = o.getCustomer() != null ? " - " + o.getCustomer().getPhone() : "";
            return new javafx.beans.property.SimpleStringProperty(name + phone);
        });

        colItems.setCellValueFactory(d ->
            new javafx.beans.property.SimpleIntegerProperty(
                d.getValue().getOrderDetails().size()).asObject());

        colTotal.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getTotalAmount()));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : fmt.format(item) + " ₫");
                setStyle("-fx-font-weight: bold;");
            }
        });

        colPayment.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getPaymentMethod() != null ? d.getValue().getPaymentMethod().toString() : ""));

        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Order.OrderStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.getDisplayName());
                setStyle(switch (item) {
                    case COMPLETED  -> "-fx-text-fill: #27ae60; -fx-font-weight:bold;";
                    case CANCELLED  -> "-fx-text-fill: #e74c3c; -fx-font-weight:bold;";
                    case PENDING    -> "-fx-text-fill: #f39c12; -fx-font-weight:bold;";
                    case SHIPPING   -> "-fx-text-fill: #2980b9; -fx-font-weight:bold;";
                    case CONFIRMED  -> "-fx-text-fill: #16a085;";
                    default         -> "-fx-text-fill: #7f8c8d;";
                });
            }
        });

        colCreatedBy.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getCreatedBy() != null ? d.getValue().getCreatedBy().getFullName() : ""));

        colDate.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getOrderDate() != null ? d.getValue().getOrderDate().format(dtf) : ""));

        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnView   = new Button("👁 Xem");
            final Button btnStatus = new Button("🔄 Trạng thái");
            final HBox box = new HBox(4, btnView, btnStatus);
            {
                btnView.setStyle("-fx-font-size:11px;-fx-padding:3 6;");
                btnStatus.setStyle("-fx-font-size:11px;-fx-padding:3 6;");
                btnView.setOnAction(e -> viewOrderDetail(getTableView().getItems().get(getIndex())));
                btnStatus.setOnAction(e -> changeOrderStatus(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tblOrders.setItems(orderList);
    }

    @FXML
    public void loadOrders() {
        try {
            List<Order> orders;

            if (dpFrom.getValue() != null && dpTo.getValue() != null) {
                LocalDateTime from = dpFrom.getValue().atStartOfDay();
                LocalDateTime to = dpTo.getValue().atTime(23, 59, 59);
                orders = orderService.getOrdersByDateRange(from, to);
            } else {
                orders = orderService.getAllOrders();
            }

            String kw = txtSearch.getText().toLowerCase().trim();
            if (!kw.isEmpty()) {
                orders = orders.stream().filter(o ->
                    o.getOrderCode().toLowerCase().contains(kw) ||
                    (o.getCustomer() != null && o.getCustomer().getFullName().toLowerCase().contains(kw))
                ).toList();
            }

            String statusStr = cbStatusFilter.getValue();
            if (statusStr != null && !statusStr.equals("Tất cả")) {
                orders = orders.stream().filter(o ->
                    o.getStatus().getDisplayName().equals(statusStr)).toList();
            }

            orderList.setAll(orders);

            lblTotal.setText("Tổng: " + orders.size() + " đơn");

            long pending = orders.stream().filter(o ->
                o.getStatus() == Order.OrderStatus.PENDING ||
                o.getStatus() == Order.OrderStatus.CONFIRMED).count();
            lblPending.setText(pending > 0 ? "⏳ " + pending + " đơn cần xử lý" : "");

            BigDecimal totalRev = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTotalRevenue.setText("Doanh thu: " + fmt.format(totalRev) + " ₫");

        } catch (Exception e) {
            MainController.showAlert("Lỗi tải danh sách đơn hàng: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML private void handleSearch() { loadOrders(); }
    @FXML private void handleFilter() { loadOrders(); }
    @FXML private void handleRefresh() {
        txtSearch.clear(); dpFrom.setValue(null); dpTo.setValue(null);
        cbStatusFilter.getSelectionModel().selectFirst();
        loadOrders();
    }

    @FXML
    private void handleCreateOrder() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/foodshop/fxml/OrderDialog.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("➕ Tạo đơn hàng mới");
            dialog.setScene(new Scene(root, 900, 650));
            dialog.showAndWait();
            loadOrders();
        } catch (Exception e) {
            MainController.showAlert("Lỗi mở form tạo đơn: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void viewOrderDetail(Order order) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/foodshop/fxml/OrderDetailView.fxml"));
            Parent root = loader.load();
            OrderDetailViewController ctrl = loader.getController();
            ctrl.setOrder(order);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("📋 Chi tiết đơn hàng: " + order.getOrderCode());
            stage.setScene(new Scene(root, 750, 600));
            stage.show();
        } catch (Exception e) {
            MainController.showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void changeOrderStatus(Order order) {
        ChoiceDialog<Order.OrderStatus> dialog = new ChoiceDialog<>(
            order.getStatus(), Order.OrderStatus.values());
        dialog.setTitle("Cập nhật trạng thái");
        dialog.setHeaderText("Đơn hàng: " + order.getOrderCode());
        dialog.setContentText("Chọn trạng thái mới:");

        dialog.showAndWait().ifPresent(newStatus -> {
            if (newStatus == order.getStatus()) return;
            try {
                orderService.updateOrderStatus(order.getId(), newStatus,
                    AuthService.getInstance().getCurrentUser());
                loadOrders();
                MainController.showAlert("Đã cập nhật trạng thái đơn hàng!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                MainController.showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
}
