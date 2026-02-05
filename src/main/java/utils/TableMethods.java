package utils;

import DB.DbConfig;
import Skeletons.PartTable;
import Skeletons.WorkTable;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableRow;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.util.converter.NumberStringConverter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TableMethods {

    public static List<String> loadTechnicianUsernames() {
        List<String> techs = new ArrayList<>();
        String sql = "SELECT username FROM technician ORDER BY username";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                techs.add(rs.getString("username"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return techs;
    }

    public static void loadRepairsTable(MFXTableView<WorkTable> repairTable, ObservableList<WorkTable> repairData, ObservableList<String> techNames) {

        repairTable.getTableColumns().clear();
        repairData.clear();

        repairTable.setTableRowFactory(workTable -> {
            MFXTableRow<WorkTable> row = new MFXTableRow<>(repairTable, workTable);
            row.setPrefHeight(60);
            return row;
        });

        MFXTableColumn<WorkTable> dateCol = new MFXTableColumn<>("Date");
        dateCol.setPrefWidth(170);
        dateCol.setRowCellFactory(item -> {
            MFXTableRowCell<WorkTable, LocalDate> cell = new MFXTableRowCell<>(WorkTable::getDate);
            DatePicker picker = new DatePicker();
            picker.valueProperty().bindBidirectional(item.dateProperty());
            cell.setGraphic(picker);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<WorkTable> techCol = new MFXTableColumn<>("Tech");
        techCol.setRowCellFactory(item->{
            MFXTableRowCell<WorkTable,String> cell = new MFXTableRowCell<>(WorkTable::getTech);
            ComboBox<String> box = new ComboBox<>();
            box.setItems(techNames);
            box.valueProperty().bindBidirectional(item.techProperty());
            cell.setGraphic(box);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<WorkTable> descCol = new MFXTableColumn<>("Description" );
        descCol.setPrefWidth(300);
        descCol.setRowCellFactory(item->{
            MFXTableRowCell<WorkTable, String> cell = new MFXTableRowCell<>(WorkTable::getDescription);
            TextArea area = new TextArea();
            area.textProperty().bindBidirectional(item.descriptionProperty());
            area.setWrapText(true);
            cell.setGraphic(area);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<WorkTable> priceCol = new MFXTableColumn<>("Price" );;
        priceCol.setRowCellFactory(item->{
            MFXTableRowCell<WorkTable,Double> cell = new MFXTableRowCell<>(WorkTable::getPrice);
            TextField field = new TextField();
            field.textProperty().bindBidirectional(item.priceProperty(), new NumberStringConverter());
            field.setAlignment(Pos.CENTER_RIGHT);
            cell.setGraphic(field);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        repairData.add(new WorkTable(LocalDate.now(), "", "", 0.0));
        repairTable.getTableColumns().addAll(dateCol, techCol, descCol, priceCol);
        repairTable.setItems(repairData);
    }

    public static void loadPartsTable(MFXTableView<PartTable> partsTable, ObservableList<PartTable> partsData) {
        partsTable.getTableColumns().clear();
        partsData.clear();

        partsTable.setTableRowFactory(partTable -> {
            MFXTableRow<PartTable> row = new MFXTableRow<>(partsTable, partTable);
            row.setPrefHeight(40);
            return row;
        });

        // -------- Name --------
        MFXTableColumn<PartTable> nameCol = new MFXTableColumn<>("Name");
        nameCol.setPrefWidth(220);
        nameCol.setRowCellFactory(item -> {
            MFXTableRowCell<PartTable, String> cell = new MFXTableRowCell<>(PartTable::getName);
            TextField tf = new TextField();
            tf.textProperty().bindBidirectional(item.nameProperty());
            cell.setGraphic(tf);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        // -------- Quantity (digits only) --------
        MFXTableColumn<PartTable> qtyCol = new MFXTableColumn<>("Quantity");
        qtyCol.setPrefWidth(140);
        qtyCol.setRowCellFactory(item -> {
            MFXTableRowCell<PartTable, Integer> cell = new MFXTableRowCell<>(PartTable::getQuantity);
            TextField tf = new TextField(String.valueOf(item.getQuantity()));

            tf.textProperty().addListener((obs, oldVal, newVal) -> {
                // only digits
                if (!newVal.matches("\\d*")) {
                    tf.setText(oldVal);
                    return;
                }

                int qty = newVal.isEmpty() ? 0 : Integer.parseInt(newVal);
                item.setQuantity(qty);

                // recalc total
                item.setTotalPrice(item.getQuantity() * item.getPrice());
            });

            cell.setGraphic(tf);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        // -------- Price (digits + one dot) --------
        MFXTableColumn<PartTable> priceCol = new MFXTableColumn<>("Price");
        priceCol.setPrefWidth(140);
        priceCol.setRowCellFactory(item -> {
            MFXTableRowCell<PartTable, Double> cell = new MFXTableRowCell<>(PartTable::getPrice);
            TextField tf = new TextField(String.valueOf(item.getPrice()));

            tf.textProperty().addListener((obs, oldVal, newVal) -> {
                // allow: "", "123", "123.", "123.45"
                if (!newVal.matches("\\d*(\\.\\d*)?")) {
                    tf.setText(oldVal);
                    return;
                }

                double price = newVal.isEmpty() || newVal.equals(".") ? 0.0 : Double.parseDouble(newVal);
                item.setPrice(price);

                // recalc total
                item.setTotalPrice(item.getQuantity() * item.getPrice());
            });

            cell.setGraphic(tf);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        // -------- Total (read-only) --------
        MFXTableColumn<PartTable> totalCol = new MFXTableColumn<>("Total");
        totalCol.setPrefWidth(140);
        totalCol.setRowCellFactory(item -> {
            MFXTableRowCell<PartTable, Double> cell = new MFXTableRowCell<>(PartTable::getTotalPrice);
            TextField tf = new TextField(String.valueOf(item.getTotalPrice()));

            tf.setEditable(false);
            tf.setFocusTraversable(false);

            // keep text in sync with model
            item.totalPriceProperty().addListener((obs, oldVal, newVal) -> {
                tf.setText(String.valueOf(newVal.doubleValue()));
            });

            cell.setGraphic(tf);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        partsData.add(new PartTable("", 0, 0.0, 0.0));
        partsTable.getTableColumns().addAll(nameCol, qtyCol, priceCol, totalCol);
        partsTable.setItems(partsData);
    }
}
