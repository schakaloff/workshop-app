package utils;

import Skeletons.PartTable;
import Skeletons.WorkTable;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableRow;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;

import java.time.LocalDate;

public class TableMethods {

    public static void loadRepairsTable(MFXTableView<WorkTable> repairTable, ObservableList<WorkTable> repairData) {
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
            cell.setGraphic(picker);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<WorkTable> techCol = new MFXTableColumn<>("Tech");
        techCol.setRowCellFactory(item->{
            MFXTableRowCell<WorkTable,String> cell = new MFXTableRowCell<>(WorkTable::getTech);
            ComboBox box = new ComboBox();
            cell.setGraphic(box);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<WorkTable> descCol = new MFXTableColumn<>("Description" );
        descCol.setPrefWidth(300);
        descCol.setRowCellFactory(item->{
            MFXTableRowCell<WorkTable, String> cell = new MFXTableRowCell<>(WorkTable::getDescription);
            TextArea area = new TextArea();
            area.setWrapText(true);
            cell.setGraphic(area);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<WorkTable> priceCol = new MFXTableColumn<>("Price" );;
        priceCol.setRowCellFactory(item->{
            MFXTableRowCell<WorkTable,Double> cell = new MFXTableRowCell<>(WorkTable::getPrice);
            TextField field = new TextField();
            field.setAlignment(Pos.CENTER_RIGHT);
            cell.setGraphic(field);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        repairData.add(new WorkTable(LocalDate.now(), "", "", 0.0));
        repairTable.getTableColumns().addAll(dateCol, techCol, descCol, priceCol);
        repairTable.setItems(repairData);
    }

    public static void loadPartsTable(MFXTableView<PartTable> partsTable, ObservableList<PartTable> partsData){
        partsTable.getTableColumns().clear();
        partsData.clear();

        partsTable.setTableRowFactory(partTable -> {
            MFXTableRow<PartTable> row = new MFXTableRow<>(partsTable, partTable);
            row.setPrefHeight(40);
            return row;
        });

        MFXTableColumn<PartTable> nameCol = new MFXTableColumn<>("Name");
        nameCol.setPrefWidth(170);
        nameCol.setRowCellFactory(item -> {
            MFXTableRowCell<PartTable,String> cell = new MFXTableRowCell<>(PartTable::getName);
            TextField nameField = new TextField();
            nameField.setPrefHeight(30);
            nameField.setPrefWidth(200);

            cell.setGraphic(nameField);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<PartTable> quantityCol = new MFXTableColumn<>("Quantity");
        quantityCol.setPrefWidth(170);
        quantityCol.setRowCellFactory(item -> {
            MFXTableRowCell<PartTable,Integer> cell = new MFXTableRowCell<>(PartTable::getQuantity);
            TextField quantityField = new TextField();
            quantityField.setPrefHeight(30);
            quantityField.setPrefWidth(200);
            cell.setGraphic(quantityField);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<PartTable> priceCol = new MFXTableColumn<>("Price");
        priceCol.setPrefWidth(170);
        priceCol.setRowCellFactory(item -> {
            MFXTableRowCell<PartTable,Double> cell = new MFXTableRowCell<>(PartTable::getPrice);
            TextField priceField = new TextField();
            priceField.setPrefHeight(30);
            priceField.setPrefWidth(200);
            cell.setGraphic(priceField);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        MFXTableColumn<PartTable> totalPriceCol = new MFXTableColumn<>("Total");
        totalPriceCol.setPrefWidth(170);
        totalPriceCol.setRowCellFactory(item -> {
            MFXTableRowCell<PartTable,Double> cell = new MFXTableRowCell<>(PartTable::getTotalPrice);
            TextField totalPriceField = new TextField();
            totalPriceField.setPrefHeight(30);
            totalPriceField.setPrefWidth(200);
            cell.setGraphic(totalPriceField);
            cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            return cell;
        });

        partsData.add(new PartTable("", 0, 0.0, 0.0));
        partsTable.getTableColumns().addAll(nameCol, quantityCol, priceCol, totalPriceCol);
        partsTable.setItems(partsData);

    }
}
