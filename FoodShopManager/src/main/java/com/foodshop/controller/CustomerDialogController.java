package com.foodshop.controller;

import com.foodshop.config.HibernateUtil;
import com.foodshop.model.Customer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.net.URL;
import java.util.ResourceBundle;

public class CustomerDialogController implements Initializable {

    @FXML private TextField txtName, txtPhone, txtEmail, txtAddress, txtDistrict, txtCity, txtPoints;
    @FXML private ComboBox<Customer.CustomerType> cbType;
    @FXML private TextArea txtNotes;
    @FXML private Label lblError;
    @FXML private Button btnSave;

    private Customer customer;
    private boolean saved = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbType.setItems(FXCollections.observableArrayList(Customer.CustomerType.values()));
        cbType.getSelectionModel().selectFirst();
    }

    public void setCustomer(Customer c) {
        this.customer = c;
        if (c != null) {
            txtName.setText(c.getFullName());
            txtPhone.setText(c.getPhone());
            if (c.getEmail() != null)    txtEmail.setText(c.getEmail());
            if (c.getAddress() != null)  txtAddress.setText(c.getAddress());
            if (c.getDistrict() != null) txtDistrict.setText(c.getDistrict());
            if (c.getCity() != null)     txtCity.setText(c.getCity());
            cbType.setValue(c.getCustomerType());
            txtPoints.setText(String.valueOf(c.getLoyaltyPoints()));
            if (c.getNotes() != null)    txtNotes.setText(c.getNotes());
            btnSave.setText("💾 Cập nhật");
        }
    }

    @FXML
    private void handleSave() {
        hideError();
        String name  = txtName.getText().trim();
        String phone = txtPhone.getText().trim();

        if (name.isEmpty())  { showError("Vui lòng nhập họ tên!"); return; }
        if (phone.isEmpty()) { showError("Vui lòng nhập số điện thoại!"); return; }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Customer c = (customer == null) ? new Customer() : session.merge(customer);
            c.setFullName(name);
            c.setPhone(phone);
            c.setEmail(txtEmail.getText().trim());
            c.setAddress(txtAddress.getText().trim());
            c.setDistrict(txtDistrict.getText().trim());
            c.setCity(txtCity.getText().trim());
            c.setCustomerType(cbType.getValue());
            c.setNotes(txtNotes.getText().trim());
            try { c.setLoyaltyPoints(Integer.parseInt(txtPoints.getText().trim())); }
            catch (NumberFormatException e) { c.setLoyaltyPoints(0); }

            if (customer == null) session.persist(c);
            tx.commit();
            saved = true;
            ((Stage) btnSave.getScene().getWindow()).close();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            showError("Lỗi: " + e.getMessage());
        }
    }

    @FXML private void handleCancel() { ((Stage) btnSave.getScene().getWindow()).close(); }

    private void showError(String msg) {
        lblError.setText("⚠ " + msg); lblError.setVisible(true); lblError.setManaged(true);
    }
    private void hideError() {
        lblError.setVisible(false); lblError.setManaged(false);
    }

    public boolean isSaved() { return saved; }
}
