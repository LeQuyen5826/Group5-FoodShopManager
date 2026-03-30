package com.foodshop.controller;

import com.foodshop.config.HibernateUtil;
import com.foodshop.service.ReportService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class ReportController implements Initializable {

    @FXML private DatePicker dpFrom, dpTo;
    @FXML private ComboBox<Integer> cbYear;
    @FXML private Label lblRevenue, lblOrderCount, lblItemsSold, lblAvgOrder;
    @FXML private TableView<Object[]> tblTopProducts;
    @FXML private TableColumn<Object[], String> rpColRank, rpColName, rpColUnit, rpColQty, rpColRevenue;
    @FXML private ListView<String> lstCategoryRevenue;

    private final ReportService reportService = ReportService.getInstance();
    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFrom.setValue(LocalDate.now().withDayOfMonth(1));
        dpTo.setValue(LocalDate.now());

        int currentYear = LocalDate.now().getYear();
        List<Integer> years = new ArrayList<>();
        for (int y = currentYear; y >= currentYear - 4; y--) years.add(y);
        cbYear.setItems(FXCollections.observableArrayList(years));
        cbYear.getSelectionModel().selectFirst();

        setupColumns();
        loadReport();
    }

    private void setupColumns() {
        rpColRank.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(tblTopProducts.getItems().indexOf(d.getValue()) + 1)));
        rpColName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue()[0] != null ? d.getValue()[0].toString() : ""));
        rpColUnit.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue()[1] != null ? d.getValue()[1].toString() : ""));
        rpColQty.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue()[2] != null ? d.getValue()[2].toString() : "0"));
        rpColRevenue.setCellValueFactory(d -> {
            Object val = d.getValue()[3];
            String formatted = val != null ? fmt.format(new BigDecimal(val.toString())) + " ₫" : "0 ₫";
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
    }

    @FXML
    public void loadReport() {
        if (dpFrom.getValue() == null || dpTo.getValue() == null) return;
        LocalDateTime from = dpFrom.getValue().atStartOfDay();
        LocalDateTime to   = dpTo.getValue().atTime(23, 59, 59);

        try {
            BigDecimal revenue = reportService.getRevenueByDay(from, to).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            long orderCount = 0;
            try (Session session = com.foodshop.config.HibernateUtil.getSessionFactory().openSession()) {
                orderCount = session.createQuery(
                    "SELECT COUNT(o) FROM Order o WHERE o.status = 'COMPLETED' " +
                    "AND o.orderDate BETWEEN :from AND :to", Long.class)
                    .setParameter("from", from).setParameter("to", to)
                    .uniqueResult();
            }

            List<Object[]> topProducts = reportService.getTopSellingProducts(10, from, to);

            long itemsSold = topProducts.stream()
                .mapToLong(r -> r[2] != null ? ((Number) r[2]).longValue() : 0L).sum();

            String avgStr = orderCount > 0
                ? fmt.format(revenue.divide(BigDecimal.valueOf(orderCount), 0, java.math.RoundingMode.HALF_UP)) + " ₫"
                : "0 ₫";

            lblRevenue.setText(fmt.format(revenue) + " ₫");
            lblOrderCount.setText(orderCount + " đơn");
            lblItemsSold.setText(itemsSold + " sp");
            lblAvgOrder.setText(avgStr);

            tblTopProducts.setItems(FXCollections.observableArrayList(topProducts));

            Map<String, BigDecimal> catRevenue = reportService.getRevenueByCategory(from, to);
            List<String> catList = new ArrayList<>();
            catRevenue.forEach((name, rev) ->
                catList.add(String.format("%-22s  %s ₫", name, fmt.format(rev))));
            lstCategoryRevenue.setItems(FXCollections.observableArrayList(catList));
            lstCategoryRevenue.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");

        } catch (Exception e) {
            MainController.showAlert("Lỗi tải báo cáo: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void loadYearReport() {
        Integer year = cbYear.getValue();
        if (year == null) return;
        dpFrom.setValue(LocalDate.of(year, 1, 1));
        dpTo.setValue(LocalDate.of(year, 12, 31));
        loadReport();
    }
}
