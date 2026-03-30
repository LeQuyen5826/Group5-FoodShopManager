package com.foodshop.controller;

import com.foodshop.config.HibernateUtil;
import com.foodshop.model.*;
import com.foodshop.service.AuthService;
import com.foodshop.service.OrderService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class OrderDialogController implements Initializable {

    @FXML private TextField txtProductSearch;
    @FXML private ComboBox<Category> cbProductCategory;
    @FXML private TableView<Product> tblAvailableProducts;
    @FXML private TableColumn<Product, String> apColCode, apColName;
    @FXML private TableColumn<Product, BigDecimal> apColPrice;
    @FXML private TableColumn<Product, Integer> apColStock;
    @FXML private TableColumn<Product, Void> apColAdd;

    @FXML private TableView<OrderDetail> tblCart;
    @FXML private TableColumn<OrderDetail, String> cartColName;
    @FXML private TableColumn<OrderDetail, BigDecimal> cartColPrice, cartColTotal;
    @FXML private TableColumn<OrderDetail, Integer> cartColQty;
    @FXML private TableColumn<OrderDetail, BigDecimal> cartColDiscount;
    @FXML private TableColumn<OrderDetail, Void> cartColRemove;

    @FXML private Label lblSubtotal, lblGrandTotal;
    @FXML private TextField txtDiscount, txtShipping;

    @FXML private TextField txtCustomerSearch;
    @FXML private ListView<String> lstCustomerResult;
    @FXML private Label lblSelectedCustomer;

    @FXML private ComboBox<Order.PaymentMethod> cbPayment;
    @FXML private TextArea txtShippingAddress, txtNotes;
    @FXML private Label lblError;

    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final OrderService orderService = OrderService.getInstance();
    private ObservableList<OrderDetail> cartItems = FXCollections.observableArrayList();
    private ObservableList<Product> productList = FXCollections.observableArrayList();

    private List<Customer> foundCustomers = new java.util.ArrayList<>();
    private Customer selectedCustomer = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupProductTable();
        setupCartTable();
        loadDropdowns();
        loadProducts();
        setupCustomerSearch();
    }

    private void setupCustomerSearch() {
        lstCustomerResult.setOnMouseClicked(e -> {
            int idx = lstCustomerResult.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < foundCustomers.size()) {
                selectedCustomer = foundCustomers.get(idx);
                lblSelectedCustomer.setText("✅ " + selectedCustomer.getFullName() + " — " + selectedCustomer.getPhone());
                lblSelectedCustomer.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold; -fx-font-size:12px;");
                lstCustomerResult.setVisible(false);
                lstCustomerResult.setManaged(false);
                txtCustomerSearch.setText(selectedCustomer.getFullName() + " - " + selectedCustomer.getPhone());
            }
        });
        lstCustomerResult.setVisible(false);
        lstCustomerResult.setManaged(false);
    }

    @FXML
    private void handleCustomerSearch() {
        String kw = txtCustomerSearch.getText().trim().toLowerCase();
        selectedCustomer = null;
        lblSelectedCustomer.setText("Khách lẻ (chưa chọn)");
        lblSelectedCustomer.setStyle("-fx-text-fill:#6b7280; -fx-font-size:12px;");

        if (kw.length() < 1) {
            lstCustomerResult.setVisible(false);
            lstCustomerResult.setManaged(false);
            return;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            foundCustomers = session.createQuery(
                "FROM Customer c WHERE LOWER(c.fullName) LIKE :kw OR c.phone LIKE :kw ORDER BY c.fullName",
                Customer.class)
                .setParameter("kw", "%" + kw + "%")
                .setMaxResults(8)
                .list();

            if (foundCustomers.isEmpty()) {
                lstCustomerResult.setItems(FXCollections.observableArrayList("Không tìm thấy khách hàng"));
            } else {
                ObservableList<String> items = FXCollections.observableArrayList();
                for (Customer c : foundCustomers) {
                    items.add(c.getFullName() + " — " + (c.getPhone() != null ? c.getPhone() : ""));
                }
                lstCustomerResult.setItems(items);
            }
            lstCustomerResult.setVisible(true);
            lstCustomerResult.setManaged(true);
        } catch (Exception e) {
            MainController.showAlert("Lỗi tìm kiếm khách: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupProductTable() {
        apColCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductCode()));
        apColName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        apColPrice.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPriceSell()));
        apColPrice.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : fmt.format(v) + " ₫");
            }
        });
        apColStock.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantityInStock()).asObject());
        apColAdd.setCellFactory(c -> new TableCell<>() {
            final Button btn = new Button("＋ Thêm");
            { btn.setStyle("-fx-font-size:11px;");
              btn.setOnAction(e -> addToCart(getTableView().getItems().get(getIndex()))); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : btn);
            }
        });
        tblAvailableProducts.setItems(productList);
        tblAvailableProducts.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Product p = tblAvailableProducts.getSelectionModel().getSelectedItem();
                if (p != null) addToCart(p);
            }
        });
    }

    private void setupCartTable() {
        cartColName.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getProduct().getName()));
        cartColPrice.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getUnitPrice()));
        cartColPrice.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : fmt.format(v) + " ₫");
            }
        });
        cartColQty.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantity()).asObject());
        cartColQty.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        cartColQty.setOnEditCommit(e -> {
            int newQty = e.getNewValue();
            if (newQty <= 0) {
                cartItems.remove(e.getRowValue());
            } else {
                e.getRowValue().setQuantity(newQty);
                e.getRowValue().calculateLineTotal();
            }
            recalcTotal();
        });
        tblCart.setEditable(true);

        cartColTotal.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getLineTotal()));
        cartColTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : fmt.format(v) + " ₫");
                setStyle("-fx-font-weight: bold;");
            }
        });

        cartColRemove.setCellFactory(c -> new TableCell<>() {
            final Button btn = new Button("✕");
            { btn.setStyle("-fx-text-fill:#e74c3c;-fx-font-size:12px;");
              btn.setOnAction(e -> { cartItems.remove(getTableView().getItems().get(getIndex())); recalcTotal(); }); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : btn);
            }
        });
        tblCart.setItems(cartItems);
    }

    private void loadDropdowns() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Category> cats = session.createQuery("FROM Category c WHERE c.active=true ORDER BY c.name", Category.class).list();
            cbProductCategory.setItems(FXCollections.observableArrayList(cats));
            cbProductCategory.getItems().add(0, null);
            cbProductCategory.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Category c) { return c == null ? "Tất cả" : c.getName(); }
                @Override public Category fromString(String s) { return null; }
            });
        }
        cbPayment.setItems(FXCollections.observableArrayList(Order.PaymentMethod.values()));
        cbPayment.getSelectionModel().selectFirst();
    }

    private void loadProducts() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String kw = txtProductSearch.getText().toLowerCase().trim();
            Category cat = cbProductCategory.getValue();

            String hql = "FROM Product p WHERE p.status = 'AVAILABLE' AND p.quantityInStock > 0";
            if (!kw.isEmpty()) hql += " AND (LOWER(p.name) LIKE :kw OR LOWER(p.productCode) LIKE :kw)";
            if (cat != null) hql += " AND p.category = :cat";
            hql += " ORDER BY p.name";

            var q = session.createQuery(hql, Product.class);
            if (!kw.isEmpty()) q.setParameter("kw", "%" + kw + "%");
            if (cat != null) q.setParameter("cat", cat);

            productList.setAll(q.list());
        }
    }

    @FXML private void handleProductSearch() { loadProducts(); }

    private void addToCart(Product product) {
        for (OrderDetail d : cartItems) {
            if (d.getProduct().getId().equals(product.getId())) {
                d.setQuantity(d.getQuantity() + 1);
                d.calculateLineTotal();
                tblCart.refresh();
                recalcTotal();
                return;
            }
        }
        OrderDetail detail = new OrderDetail(product, 1, product.getPriceSell());
        cartItems.add(detail);
        recalcTotal();
    }

    @FXML
    public void recalcTotal() {
        BigDecimal subtotal = cartItems.stream()
            .map(OrderDetail::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblSubtotal.setText(fmt.format(subtotal) + " ₫");

        BigDecimal discount = parseMoney(txtDiscount.getText());
        BigDecimal shipping = parseMoney(txtShipping.getText());
        BigDecimal grand = subtotal.subtract(discount).add(shipping);
        if (grand.compareTo(BigDecimal.ZERO) < 0) grand = BigDecimal.ZERO;
        lblGrandTotal.setText(fmt.format(grand) + " ₫");
        lblGrandTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
    }

    @FXML
    private void handleSaveOrder() {
        hideError();
        if (cartItems.isEmpty()) { showError("Giỏ hàng trống! Vui lòng thêm sản phẩm."); return; }

        try {
            Order order = new Order();
            order.setCustomer(selectedCustomer); // null = khách lẻ
            order.setPaymentMethod(cbPayment.getValue());
            order.setShippingAddress(txtShippingAddress.getText().trim());
            order.setNotes(txtNotes.getText().trim());
            order.setDiscountAmount(parseMoney(txtDiscount.getText()));
            order.setShippingFee(parseMoney(txtShipping.getText()));
            cartItems.forEach(d -> { d.setOrder(order); order.getOrderDetails().add(d); });
            order.recalculateTotal();

            orderService.createOrder(order, AuthService.getInstance().getCurrentUser());
            MainController.showAlert("✅ Tạo đơn hàng thành công!\nMã đơn: " + order.getOrderCode(),
                Alert.AlertType.INFORMATION);
            ((Stage) lblGrandTotal.getScene().getWindow()).close();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML private void handleAddCustomer() {
        MainController.showAlert("Chức năng thêm khách hàng nhanh — vui lòng vào trang Khách hàng để thêm mới.", Alert.AlertType.INFORMATION);
    }

    @FXML private void handleCancel() {
        ((Stage) lblGrandTotal.getScene().getWindow()).close();
    }

    private BigDecimal parseMoney(String s) {
        try { return new BigDecimal(s.replaceAll("[^0-9.]", "")); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    private void showError(String msg) {
        lblError.setText("⚠ " + msg);
        lblError.setVisible(true); lblError.setManaged(true);
    }
    private void hideError() {
        lblError.setVisible(false); lblError.setManaged(false);
    }
}
