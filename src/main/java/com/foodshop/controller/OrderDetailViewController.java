package com.foodshop.controller;

import com.foodshop.model.Order;
import com.foodshop.model.OrderDetail;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class OrderDetailViewController implements Initializable {

    @FXML private Label lblOrderCode, lblCustomer, lblOrderDate, lblStatus;
    @FXML private Label lblPayment, lblShipping, lblNotes, lblCreatedBy;
    @FXML private Label lblSubtotal, lblDiscount, lblShippingFee, lblTotal;
    @FXML private TableView<OrderDetail> tblDetails;
    @FXML private TableColumn<OrderDetail, String> colProduct, colUnit;
    @FXML private TableColumn<OrderDetail, Integer> colQty;
    @FXML private TableColumn<OrderDetail, BigDecimal> colUnitPrice, colLineTotal;

    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colProduct.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getProduct().getName()));
        colUnit.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getProduct().getUnit()));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colUnitPrice.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : fmt.format(v) + " ₫");
            }
        });
        colLineTotal.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        colLineTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : fmt.format(v) + " ₫");
                setStyle("-fx-font-weight:bold;");
            }
        });
    }

    public void setOrder(Order order) {
        lblOrderCode.setText(order.getOrderCode());
        lblCustomer.setText(order.getCustomer() != null ?
            order.getCustomer().getFullName() + " - " + order.getCustomer().getPhone() : "Khách lẻ");
        lblOrderDate.setText(order.getOrderDate() != null ? order.getOrderDate().format(dtf) : "");
        lblStatus.setText(order.getStatus().getDisplayName());
        lblPayment.setText(order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : "");
        lblShipping.setText(order.getShippingAddress() != null ? order.getShippingAddress() : "");
        lblNotes.setText(order.getNotes() != null ? order.getNotes() : "");
        lblCreatedBy.setText(order.getCreatedBy() != null ? order.getCreatedBy().getFullName() : "");
        lblSubtotal.setText(fmt.format(order.getSubtotal()) + " ₫");
        lblDiscount.setText("- " + fmt.format(order.getDiscountAmount()) + " ₫");
        lblShippingFee.setText("+ " + fmt.format(order.getShippingFee()) + " ₫");
        lblTotal.setText(fmt.format(order.getTotalAmount()) + " ₫");
        tblDetails.setItems(FXCollections.observableArrayList(order.getOrderDetails()));
    }
}
