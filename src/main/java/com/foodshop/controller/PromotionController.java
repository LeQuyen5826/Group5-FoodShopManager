package com.foodshop.controller;

import com.foodshop.config.HibernateUtil;
import com.foodshop.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class PromotionController implements Initializable {

    @FXML private TextField txtSearchPromo;
    @FXML private ComboBox<String> cbCustomerType;
    @FXML private TableView<PromotionItem> tblPromotions;
    @FXML private TableColumn<PromotionItem, String> proColName, proColType, proColDiscount,
            proColMinOrder, proColDateFrom, proColDateTo, proColActive;
    @FXML private TableColumn<PromotionItem, Void> proColActions;

    @FXML private Label lblPromoFormTitle;
    @FXML private TextField txtPromoName, txtDiscountValue, txtMinOrder;
    @FXML private ComboBox<String> cbPromoCustomerType, cbDiscountType;
    @FXML private DatePicker dpPromoFrom, dpPromoTo;
    @FXML private TextArea txtPromoDesc;
    @FXML private CheckBox chkPromoActive;
    @FXML private Button btnSavePromo;

    @FXML private TextField txtSearchPoints;
    @FXML private ComboBox<String> cbVipFilter;
    @FXML private TableView<Object[]> tblCustomerPoints;
    @FXML private TableColumn<Object[], String> ptColName, ptColPhone, ptColType, ptColPoints,
            ptColOrders, ptColSpent, ptColLevel;
    @FXML private Label lblPointsTotal, lblVipCount;

    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private ObservableList<PromotionItem> promoList = FXCollections.observableArrayList();
    private PromotionItem editingPromo = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Dropdowns
        cbCustomerType.setItems(FXCollections.observableArrayList("Tất cả", "Khách thường", "Khách VIP"));
        cbCustomerType.getSelectionModel().selectFirst();
        cbCustomerType.setOnAction(e -> loadPromotions());

        cbPromoCustomerType.setItems(FXCollections.observableArrayList("Tất cả", "Khách thường", "Khách VIP"));
        cbPromoCustomerType.getSelectionModel().selectFirst();

        cbDiscountType.setItems(FXCollections.observableArrayList("Giảm theo %", "Giảm tiền cố định"));
        cbDiscountType.getSelectionModel().selectFirst();

        cbVipFilter.setItems(FXCollections.observableArrayList("Tất cả", "Khách thường", "Khách VIP"));
        cbVipFilter.getSelectionModel().selectFirst();
        cbVipFilter.setOnAction(e -> loadCustomerPoints());

        dpPromoFrom.setValue(LocalDate.now());
        dpPromoTo.setValue(LocalDate.now().plusMonths(1));

        setupPromoTable();
        setupPointsTable();
        loadPromotions();
        loadCustomerPoints();
    }

    private void setupPromoTable() {
        proColName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().name));
        proColType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().customerType));
        proColType.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("Khách VIP".equals(v) ? "-fx-text-fill:#f59e0b; -fx-font-weight:bold;" :
                         "Tất cả".equals(v)    ? "-fx-text-fill:#3b82f6;"                       : "");
            }
        });
        proColDiscount.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().discountDisplay));
        proColMinOrder.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().minOrder.compareTo(BigDecimal.ZERO) > 0 ? fmt.format(d.getValue().minOrder) + " ₫" : "—"));
        proColDateFrom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().dateFrom != null ? d.getValue().dateFrom.format(dtf) : ""));
        proColDateTo.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().dateTo != null ? d.getValue().dateTo.format(dtf) : ""));
        proColActive.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().active ? "✅ Đang chạy" : "⏸ Tạm dừng"));
        proColActive.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle(v.contains("Đang") ? "-fx-text-fill:#16a34a;" : "-fx-text-fill:#6b7280;");
            }
        });
        proColActions.setCellFactory(c -> new TableCell<>() {
            final Button btnEdit = new Button("✏");
            final Button btnDel  = new Button("🗑");
            final HBox box = new HBox(4, btnEdit, btnDel);
            {
                btnEdit.setStyle("-fx-font-size:11px;-fx-padding:3 6;");
                btnDel.setStyle("-fx-font-size:11px;-fx-padding:3 6;-fx-text-fill:#dc2626;");
                btnEdit.setOnAction(e -> loadPromoToForm(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> deletePromo(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : box);
            }
        });
        tblPromotions.setItems(promoList);
    }

    private void setupPointsTable() {
        ptColName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue()[0] != null ? d.getValue()[0].toString() : ""));
        ptColPhone.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue()[1] != null ? d.getValue()[1].toString() : ""));
        ptColType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue()[2] != null ? d.getValue()[2].toString() : "Khách thường"));
        ptColType.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v);
                setStyle("VIP".equalsIgnoreCase(v) ? "-fx-text-fill:#f59e0b;-fx-font-weight:bold;" : "");
            }
        });
        ptColPoints.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue()[3] != null ? d.getValue()[3].toString() + " pts" : "0 pts"));
        ptColOrders.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue()[4] != null ? d.getValue()[4].toString() : "0"));
        ptColSpent.setCellValueFactory(d -> {
            Object val = d.getValue()[5];
            String s = val != null ? fmt.format(new BigDecimal(val.toString())) + " ₫" : "0 ₫";
            return new javafx.beans.property.SimpleStringProperty(s);
        });
        ptColLevel.setCellValueFactory(d -> {
            // Xếp hạng theo điểm: Đồng < 100, Bạc < 500, Vàng < 2000, Kim cương >= 2000
            long pts = d.getValue()[3] != null ? ((Number)d.getValue()[3]).longValue() : 0;
            String level = pts >= 2000 ? "💎 Kim cương" : pts >= 500 ? "🥇 Vàng" :
                           pts >= 100  ? "🥈 Bạc"       : "🥉 Đồng";
            return new javafx.beans.property.SimpleStringProperty(level);
        });
    }

    @FXML
    public void loadPromotions() {
        String kw = txtSearchPromo != null ? txtSearchPromo.getText().toLowerCase().trim() : "";
        String type = cbCustomerType != null ? cbCustomerType.getValue() : "Tất cả";
        ObservableList<PromotionItem> filtered = FXCollections.observableArrayList();
        for (PromotionItem p : promoList) {
            boolean matchKw   = kw.isEmpty() || p.name.toLowerCase().contains(kw);
            boolean matchType = "Tất cả".equals(type) || p.customerType.equals(type);
            if (matchKw && matchType) filtered.add(p);
        }
        tblPromotions.setItems(filtered);
    }

    @FXML
    private void handleSearchPromo() { loadPromotions(); }

    @FXML
    public void loadCustomerPoints() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String kw    = txtSearchPoints != null ? txtSearchPoints.getText().trim().toLowerCase() : "";
            String vipFilter = cbVipFilter != null ? cbVipFilter.getValue() : "Tất cả";

            String hql = "SELECT c.fullName, c.phone, c.customerType, " +
                "CAST(COALESCE(SUM(o.totalAmount), 0) / 10000 AS long), " +
                "COUNT(o.id), COALESCE(SUM(o.totalAmount), 0) " +
                "FROM Customer c LEFT JOIN c.orders o WITH o.status = 'COMPLETED' " +
                "WHERE 1=1";
            if (!kw.isEmpty()) hql += " AND (LOWER(c.fullName) LIKE :kw OR c.phone LIKE :kw)";
            if (!"Tất cả".equals(vipFilter)) hql += " AND c.customerType = :vip";
            hql += " GROUP BY c.id, c.fullName, c.phone, c.customerType ORDER BY 4 DESC";

            var q = session.createQuery(hql, Object[].class);
            if (!kw.isEmpty()) q.setParameter("kw", "%" + kw + "%");
            if (!"Tất cả".equals(vipFilter)) q.setParameter("vip", vipFilter);

            List<Object[]> rows = q.list();
            tblCustomerPoints.setItems(FXCollections.observableArrayList(rows));

            long total = rows.size();
            long vip = rows.stream().filter(r -> "VIP".equalsIgnoreCase(r[2] != null ? r[2].toString() : "")).count();
            if (lblPointsTotal != null) lblPointsTotal.setText("Tổng: " + total + " khách");
            if (lblVipCount != null) lblVipCount.setText(vip > 0 ? "⭐ " + vip + " khách VIP" : "");
        } catch (Exception e) {
            // Fallback: nếu Customer chưa có field customerType thì query đơn giản hơn
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                String hql2 = "SELECT c.fullName, c.phone, 'Khách thường', " +
                    "CAST(COALESCE(SUM(o.totalAmount), 0) / 10000 AS long), " +
                    "COUNT(o.id), COALESCE(SUM(o.totalAmount), 0) " +
                    "FROM Customer c LEFT JOIN c.orders o WITH o.status = 'COMPLETED' " +
                    "GROUP BY c.id, c.fullName, c.phone ORDER BY 4 DESC";
                List<Object[]> rows = session.createQuery(hql2, Object[].class).list();
                tblCustomerPoints.setItems(FXCollections.observableArrayList(rows));
                if (lblPointsTotal != null) lblPointsTotal.setText("Tổng: " + rows.size() + " khách");
            } catch (Exception ex) {
                MainController.showAlert("Lỗi tải điểm tích lũy: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML private void handleSearchPoints() { loadCustomerPoints(); }

    @FXML
    private void handleSavePromo() {
        String name = txtPromoName.getText().trim();
        if (name.isEmpty()) { MainController.showAlert("Vui lòng nhập tên chương trình!", Alert.AlertType.WARNING); return; }
        if (dpPromoFrom.getValue() == null || dpPromoTo.getValue() == null) {
            MainController.showAlert("Vui lòng chọn thời gian!", Alert.AlertType.WARNING); return; }
        String discStr = txtDiscountValue.getText().trim();
        if (discStr.isEmpty()) { MainController.showAlert("Vui lòng nhập giá trị giảm!", Alert.AlertType.WARNING); return; }

        BigDecimal discVal;
        try { discVal = new BigDecimal(discStr.replaceAll("[^0-9.]", "")); }
        catch (Exception ex) { MainController.showAlert("Giá trị giảm không hợp lệ!", Alert.AlertType.WARNING); return; }

        boolean isPercent = "Giảm theo %".equals(cbDiscountType.getValue());
        String discDisplay = isPercent ? discVal + "%" : fmt.format(discVal) + " ₫";

        BigDecimal minOrder = BigDecimal.ZERO;
        try { minOrder = new BigDecimal(txtMinOrder.getText().replaceAll("[^0-9.]", "")); }
        catch (Exception ignored) {}

        if (editingPromo != null) {
            editingPromo.name = name;
            editingPromo.customerType = cbPromoCustomerType.getValue();
            editingPromo.discountDisplay = discDisplay;
            editingPromo.minOrder = minOrder;
            editingPromo.dateFrom = dpPromoFrom.getValue();
            editingPromo.dateTo = dpPromoTo.getValue();
            editingPromo.active = chkPromoActive.isSelected();
            editingPromo.description = txtPromoDesc.getText().trim();
            tblPromotions.refresh();
            MainController.showAlert("✅ Cập nhật khuyến mãi thành công!", Alert.AlertType.INFORMATION);
        } else {
            PromotionItem item = new PromotionItem();
            item.name = name;
            item.customerType = cbPromoCustomerType.getValue();
            item.discountDisplay = discDisplay;
            item.minOrder = minOrder;
            item.dateFrom = dpPromoFrom.getValue();
            item.dateTo = dpPromoTo.getValue();
            item.active = chkPromoActive.isSelected();
            item.description = txtPromoDesc.getText().trim();
            promoList.add(item);
            MainController.showAlert("✅ Thêm khuyến mãi thành công!", Alert.AlertType.INFORMATION);
        }
        handleClearPromo();
        loadPromotions();
    }

    private void loadPromoToForm(PromotionItem p) {
        editingPromo = p;
        lblPromoFormTitle.setText("✏ Sửa khuyến mãi");
        txtPromoName.setText(p.name);
        cbPromoCustomerType.setValue(p.customerType);
        txtDiscountValue.setText(p.discountDisplay.replaceAll("[^0-9.]", ""));
        cbDiscountType.setValue(p.discountDisplay.contains("%") ? "Giảm theo %" : "Giảm tiền cố định");
        txtMinOrder.setText(p.minOrder.compareTo(BigDecimal.ZERO) > 0 ? p.minOrder.toPlainString() : "");
        dpPromoFrom.setValue(p.dateFrom);
        dpPromoTo.setValue(p.dateTo);
        txtPromoDesc.setText(p.description != null ? p.description : "");
        chkPromoActive.setSelected(p.active);
        btnSavePromo.setText("💾 Cập nhật");
    }

    private void deletePromo(PromotionItem p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Xóa chương trình \"" + p.name + "\"?");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            promoList.remove(p);
            loadPromotions();
        }
    }

    @FXML
    private void handleClearPromo() {
        editingPromo = null;
        lblPromoFormTitle.setText("➕ Thêm khuyến mãi");
        txtPromoName.clear(); txtDiscountValue.clear(); txtMinOrder.clear(); txtPromoDesc.clear();
        cbPromoCustomerType.getSelectionModel().selectFirst();
        cbDiscountType.getSelectionModel().selectFirst();
        dpPromoFrom.setValue(LocalDate.now());
        dpPromoTo.setValue(LocalDate.now().plusMonths(1));
        chkPromoActive.setSelected(true);
        btnSavePromo.setText("➕ Thêm mới");
    }

    public static class PromotionItem {
        public String name, customerType, discountDisplay, description;
        public BigDecimal minOrder = BigDecimal.ZERO;
        public LocalDate dateFrom, dateTo;
        public boolean active = true;
    }
}
