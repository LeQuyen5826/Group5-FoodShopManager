package com.foodshop.controller;

import com.foodshop.config.HibernateUtil;
import com.foodshop.model.User;
import com.foodshop.service.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbRoleFilter;
    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, String> colUsername, colFullName, colEmail, colPhone, colRole;
    @FXML private TableColumn<User, Boolean> colActive;
    @FXML private TableColumn<User, Void> colActions;
    @FXML private Label lblTotal;

    @FXML private TextField txtUsername, txtFullName, txtEmail, txtPhone, txtPassword;
    @FXML private ComboBox<User.Role> cbRole;
    @FXML private CheckBox chkActive;
    @FXML private Button btnSave;
    @FXML private Label lblFormTitle;

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private User editingUser = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        cbRoleFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "Quản trị viên", "Nhân viên", "Thủ kho"));
        cbRoleFilter.getSelectionModel().selectFirst();
        cbRole.setItems(FXCollections.observableArrayList(User.Role.values()));
        cbRole.getSelectionModel().selectFirst();
        chkActive.setSelected(true);
        loadUsers();
    }

    private void setupTable() {
        colUsername.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getUsername()));
        colFullName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getFullName()));
        colEmail.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getEmail() != null ? d.getValue().getEmail() : ""));
        colPhone.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getPhone() != null ? d.getValue().getPhone() : ""));
        colRole.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getRole().toString()));
        colRole.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "Quản trị viên" -> "-fx-text-fill: #7c3aed; -fx-font-weight:bold;";
                    case "Thủ kho"       -> "-fx-text-fill: #0369a1; -fx-font-weight:bold;";
                    default              -> "-fx-text-fill: #374151;";
                });
            }
        });
        colActive.setCellValueFactory(d -> new javafx.beans.property.SimpleBooleanProperty(
            d.getValue().isActive()).asObject());
        colActive.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v ? "✅ Hoạt động" : "🔴 Tạm khóa");
                setStyle(v ? "-fx-text-fill:#16a34a;" : "-fx-text-fill:#dc2626;");
            }
        });

        colActions.setCellFactory(c -> new TableCell<>() {
            final Button btnEdit   = new Button("✏ Sửa");
            final Button btnDelete = new Button("🗑 Xóa");
            final HBox box = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.setStyle("-fx-font-size:11px;-fx-padding:3 8;");
                btnDelete.setStyle("-fx-font-size:11px;-fx-padding:3 6;-fx-text-fill:#dc2626;-fx-border-color:#dc2626;-fx-border-radius:4;-fx-background-color:white;-fx-border-width:1;");
                btnEdit.setOnAction(e -> loadUserToForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteUser(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        tblUsers.setItems(userList);
    }

    @FXML
    public void loadUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String kw = txtSearch != null ? txtSearch.getText().toLowerCase().trim() : "";
            String roleStr = cbRoleFilter != null ? cbRoleFilter.getValue() : "Tất cả";

            String hql = "FROM User u WHERE 1=1";
            if (!kw.isEmpty()) hql += " AND (LOWER(u.fullName) LIKE :kw OR LOWER(u.username) LIKE :kw)";
            if (roleStr != null && !roleStr.equals("Tất cả")) {
                User.Role role = switch (roleStr) {
                    case "Quản trị viên" -> User.Role.ADMIN;
                    case "Thủ kho"       -> User.Role.WAREHOUSE;
                    default              -> User.Role.STAFF;
                };
                hql += " AND u.role = :role";
                var q = session.createQuery(hql + " ORDER BY u.fullName", User.class);
                if (!kw.isEmpty()) q.setParameter("kw", "%" + kw + "%");
                q.setParameter("role", role);
                userList.setAll(q.list());
            } else {
                var q = session.createQuery(hql + " ORDER BY u.fullName", User.class);
                if (!kw.isEmpty()) q.setParameter("kw", "%" + kw + "%");
                userList.setAll(q.list());
            }
            if (lblTotal != null)
                lblTotal.setText("Tổng: " + userList.size() + " người dùng");
        } catch (Exception e) {
            MainController.showAlert("Lỗi tải danh sách: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML private void handleSearch() { loadUsers(); }
    @FXML private void handleFilter() { loadUsers(); }

    @FXML
    private void handleRefresh() {
        txtSearch.clear();
        cbRoleFilter.getSelectionModel().selectFirst();
        loadUsers();
        clearForm();
    }

    private void loadUserToForm(User user) {
        editingUser = user;
        lblFormTitle.setText("✏ Sửa người dùng");
        txtUsername.setText(user.getUsername());
        txtUsername.setDisable(true);
        txtFullName.setText(user.getFullName());
        txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        txtPhone.setText(user.getPhone() != null ? user.getPhone() : "");
        txtPassword.clear();
        txtPassword.setPromptText("Để trống nếu không đổi mật khẩu");
        cbRole.setValue(user.getRole());
        chkActive.setSelected(user.isActive());
        btnSave.setText("💾 Cập nhật");
    }

    @FXML
    private void handleSave() {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String email    = txtEmail.getText().trim();
        String phone    = txtPhone.getText().trim();
        String password = txtPassword.getText().trim();
        User.Role role  = cbRole.getValue();

        if (username.isEmpty() || fullName.isEmpty()) {
            MainController.showAlert("Vui lòng nhập tên đăng nhập và họ tên!", Alert.AlertType.WARNING);
            return;
        }
        if (editingUser == null && password.isEmpty()) {
            MainController.showAlert("Vui lòng nhập mật khẩu cho tài khoản mới!", Alert.AlertType.WARNING);
            return;
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            if (editingUser == null) {
                Long count = session.createQuery(
                    "SELECT COUNT(u) FROM User u WHERE u.username = :un", Long.class)
                    .setParameter("un", username).uniqueResult();
                if (count > 0) {
                    MainController.showAlert("Tên đăng nhập '" + username + "' đã tồn tại!", Alert.AlertType.WARNING);
                    return;
                }
                User user = new User(username, BCrypt.hashpw(password, BCrypt.gensalt()), fullName, role);
                user.setEmail(email.isEmpty() ? null : email);
                user.setPhone(phone.isEmpty() ? null : phone);
                user.setActive(chkActive.isSelected());
                session.persist(user);
                MainController.showAlert("✅ Thêm tài khoản thành công!", Alert.AlertType.INFORMATION);
            } else {
                User user = session.get(User.class, editingUser.getId());
                user.setFullName(fullName);
                user.setEmail(email.isEmpty() ? null : email);
                user.setPhone(phone.isEmpty() ? null : phone);
                user.setRole(role);
                user.setActive(chkActive.isSelected());
                if (!password.isEmpty()) {
                    user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
                }
                session.merge(user);
                MainController.showAlert("✅ Cập nhật tài khoản thành công!", Alert.AlertType.INFORMATION);
            }
            tx.commit();
            clearForm();
            loadUsers();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            MainController.showAlert("Lỗi lưu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleClear() { clearForm(); }

    private void clearForm() {
        editingUser = null;
        lblFormTitle.setText("➕ Thêm người dùng");
        txtUsername.clear();
        txtUsername.setDisable(false);
        txtFullName.clear();
        txtEmail.clear();
        txtPhone.clear();
        txtPassword.clear();
        txtPassword.setPromptText("Mật khẩu");
        cbRole.getSelectionModel().selectFirst();
        chkActive.setSelected(true);
        btnSave.setText("➕ Thêm mới");
    }

    private void handleDeleteUser(User user) {
        // Không cho xóa chính mình
        User currentUser = AuthService.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(user.getId())) {
            MainController.showAlert("❌ Không thể xóa tài khoản đang đăng nhập!", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa tài khoản: " + user.getFullName() + " (" + user.getUsername() + ")?");
        confirm.setContentText("⚠ Hành động này không thể hoàn tác. Tài khoản sẽ bị xóa vĩnh viễn!");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Transaction tx = null;
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                tx = session.beginTransaction();
                User u = session.get(User.class, user.getId());
                if (u != null) session.remove(u);
                tx.commit();
                MainController.showAlert("✅ Đã xóa tài khoản: " + user.getFullName(), Alert.AlertType.INFORMATION);
                loadUsers();
                clearForm();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                MainController.showAlert("Lỗi xóa tài khoản: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
}
