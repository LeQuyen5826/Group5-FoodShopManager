package com.foodshop.controller;

import com.foodshop.model.Order;
import com.foodshop.model.Product;
import com.foodshop.service.OrderService;
import com.foodshop.service.ProductService;
import com.foodshop.service.ReportService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // Thẻ thống kê
    @FXML
    private Label lblTodayOrders;
    @FXML
    private Label lblTodayRevenue;
    @FXML
    private Label lblMonthRevenue;
    @FXML
    private Label lblTotalProducts;
    @FXML
    private Label lblLowStock;
    @FXML
    private Label lblTotalCustomers;
    @FXML
    private Label lblPendingOrders;

    // Bảng đơn hàng gần đây
    @FXML
    private TableView<Order> tblRecentOrders;
    @FXML
    private TableColumn<Order, String> colOrderCode;
    @FXML
    private TableColumn<Order, String> colOrderCustomer;
    @FXML
    private TableColumn<Order, BigDecimal> colOrderTotal;
    @FXML
    private TableColumn<Order, Order.OrderStatus> colOrderStatus;
    @FXML
    private TableColumn<Order, String> colOrderDate;

    // Danh sách cảnh báo kho
    @FXML
    private ListView<String> lstLowStock;

    private final ReportService reportService = ReportService.getInstance();
    private final OrderService orderService = OrderService.getInstance();
    private final ProductService productService = ProductService.getInstance();

    // Format tiền VNĐ
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        loadDashboardData();
    }

    private void setupTableColumns() {
        colOrderCode.setCellValueFactory(new PropertyValueFactory<>("orderCode"));

        colOrderCustomer.setCellValueFactory(data -> {
            Order order = data.getValue();
            String name = order.getCustomer() != null
                    ? order.getCustomer().getFullName() : "Khách lẻ";
            return new javafx.beans.property.SimpleStringProperty(name);
        });

        colOrderTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colOrderTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : currencyFormat.format(item) + " ₫");
            }
        });

        colOrderStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOrderStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Order.OrderStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.getDisplayName());
                    String color = switch (item) {
                        case COMPLETED ->
                            "-fx-text-fill: #27ae60;";
                        case CANCELLED ->
                            "-fx-text-fill: #e74c3c;";
                        case PENDING ->
                            "-fx-text-fill: #f39c12;";
                        case SHIPPING ->
                            "-fx-text-fill: #2980b9;";
                        default ->
                            "-fx-text-fill: #7f8c8d;";
                    };
                    setStyle(color + " -fx-font-weight: bold;");
                }
            }
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colOrderDate.setCellValueFactory(data -> {
            Order order = data.getValue();
            String date = order.getOrderDate() != null
                    ? order.getOrderDate().format(dtf) : "";
            return new javafx.beans.property.SimpleStringProperty(date);
        });
    }

    private void loadDashboardData() {
        try {
            ReportService.DashboardStats stats = reportService.getDashboardStats();

            lblTodayOrders.setText(String.valueOf(stats.todayOrders));
            lblTodayRevenue.setText(currencyFormat.format(stats.todayRevenue) + " ₫");
            lblMonthRevenue.setText("Tháng này: " + currencyFormat.format(stats.monthRevenue) + " ₫");
            lblTotalProducts.setText(String.valueOf(stats.totalProducts));
            lblLowStock.setText(stats.lowStockProducts > 0
                    ? "⚠ " + stats.lowStockProducts + " sắp hết hàng"
                    : "✓ Tồn kho ổn định");
            lblTotalCustomers.setText(String.valueOf(stats.totalCustomers));
            lblPendingOrders.setText(stats.pendingOrders + " đơn chờ xử lý");

            List<Order> recentOrders = orderService.getAllOrders();
            int limit = Math.min(recentOrders.size(), 10);
            tblRecentOrders.setItems(
                    FXCollections.observableArrayList(recentOrders.subList(0, limit))
            );

            List<Product> lowStockList = productService.getLowStockProducts();
            List<String> lowStockItems = lowStockList.stream()
                    .map(p -> "⚠ " + p.getName() + " — còn " + p.getQuantityInStock() + " " + p.getUnit())
                    .toList();
            lstLowStock.setItems(FXCollections.observableArrayList(lowStockItems));

        } catch (Exception e) {
            System.err.println("Lỗi load Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void viewAllOrders() {

        try {
            javafx.scene.Node node = tblRecentOrders;
            MainController mainCtrl = (MainController) node.getScene().getWindow()
                    .getUserData();
            if (mainCtrl != null) {
                mainCtrl.showOrders();
            }
        } catch (Exception ignored) {
        }
    }
}
