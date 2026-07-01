    package utils;

    import DB.DbConfig;
    import Skeletons.PartTable;
    import Skeletons.Technicians;
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

    import javafx.beans.binding.Bindings;
    import javafx.geometry.Insets;
    import javafx.scene.Scene;
    import javafx.scene.input.KeyCode;
    import javafx.scene.layout.HBox;
    import javafx.scene.layout.Priority;
    import javafx.scene.layout.Region;
    import javafx.scene.layout.StackPane;
    import javafx.scene.layout.VBox;
    import javafx.scene.paint.Color;
    import javafx.stage.Modality;
    import javafx.stage.Stage;
    import javafx.stage.StageStyle;
    import javafx.stage.Window;

    public class TableMethods {
        public static ObservableList<String> loadRoleOptions() {
            return FXCollections.observableArrayList(
                    "ADMIN",
                    "TECHNICIAN",
                    "ACCOUNTANT"
            );
        }

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
            techCol.setRowCellFactory(item -> {
                MFXTableRowCell<WorkTable, String> cell = new MFXTableRowCell<>(WorkTable::getTech);
                ComboBox<String> box = new ComboBox<>();
                box.setItems(techNames);
                box.valueProperty().bindBidirectional(item.techProperty());
                cell.setGraphic(box);
                cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                return cell;
            });

            MFXTableColumn<WorkTable> descCol = new MFXTableColumn<>("Description");
            descCol.setPrefWidth(300);
            descCol.setRowCellFactory(item -> {
                MFXTableRowCell<WorkTable, String> cell = new MFXTableRowCell<>(WorkTable::getDescription);

                Label preview = new Label();
                preview.textProperty().bind(Bindings.createStringBinding(() -> {
                    String d = item.getDescription();
                    if (d == null || d.isBlank()) return "Click to add description...";
                    return d.length() > 55 ? d.substring(0, 52) + "…" : d;
                }, item.descriptionProperty()));
                preview.setMaxWidth(Double.MAX_VALUE);
                preview.setCursor(javafx.scene.Cursor.HAND);
                preview.setStyle(
                    "-fx-background-color: #eef8f9;" +
                    "-fx-text-fill: #555;" +
                    "-fx-padding: 6 10 6 10;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-color: #0097A7;" +
                    "-fx-border-width: 0 0 0 3;" +
                    "-fx-font-size: 12;"
                );
                preview.setOnMouseClicked(e -> openDescriptionPopup(item, preview));

                cell.setGraphic(preview);
                cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                return cell;
            });

            MFXTableColumn<WorkTable> priceCol = new MFXTableColumn<>("Price");
            priceCol.setRowCellFactory(item -> {
                MFXTableRowCell<WorkTable, Double> cell = new MFXTableRowCell<>(WorkTable::getPrice);
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
                    if (!newVal.matches("\\d*")) {
                        tf.setText(oldVal);
                        return;
                    }
                    int qty = newVal.isEmpty() ? 0 : Integer.parseInt(newVal);
                    item.setQuantity(qty);
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
                    if (!newVal.matches("\\d*(\\.\\d*)?")) {
                        tf.setText(oldVal);
                        return;
                    }
                    double price = newVal.isEmpty() || newVal.equals(".") ? 0.0 : Double.parseDouble(newVal);
                    item.setPrice(price);
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

        public static ObservableList<Technicians> loadTechniciansWithRoles() {
            ObservableList<Technicians> techData = FXCollections.observableArrayList();

            String sql = "SELECT username, first_name, last_name, role FROM technician ORDER BY username";

            try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    techData.add(new Technicians(
                            rs.getString("username"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("role")
                    ));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return techData;
        }

        public static void loadTechniciansRolesTable(
                MFXTableView<Technicians> techTable,
                ObservableList<Technicians> techData
        ) {
            techTable.getTableColumns().clear();

            ObservableList<String> roleOptions = loadRoleOptions();

            techTable.setTableRowFactory(item -> {
                MFXTableRow<Technicians> row = new MFXTableRow<>(techTable, item);
                row.setPrefHeight(45);
                return row;
            });

            MFXTableColumn<Technicians> usernameCol = new MFXTableColumn<>("Username");
            usernameCol.setMinWidth(150);
            usernameCol.setRowCellFactory(item ->
                    new MFXTableRowCell<>(Technicians::getUserName)
            );

            MFXTableColumn<Technicians> firstNameCol = new MFXTableColumn<>("First Name");
            firstNameCol.setMinWidth(170);
            firstNameCol.setRowCellFactory(item ->
                    new MFXTableRowCell<>(Technicians::getFName)
            );

            MFXTableColumn<Technicians> lastNameCol = new MFXTableColumn<>("Last Name");
            lastNameCol.setMinWidth(170);
            lastNameCol.setRowCellFactory(item ->
                    new MFXTableRowCell<>(Technicians::getLName)
            );

            MFXTableColumn<Technicians> roleCol = new MFXTableColumn<>("Role");
            roleCol.setMinWidth(180);
            roleCol.setRowCellFactory(item -> {
                MFXTableRowCell<Technicians, String> cell = new MFXTableRowCell<>(Technicians::getRole);

                ComboBox<String> box = new ComboBox<>();
                box.setItems(roleOptions);
                box.valueProperty().bindBidirectional(item.roleProperty());
                box.setMaxWidth(Double.MAX_VALUE);

                cell.setGraphic(box);
                cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                return cell;
            });

            techTable.getTableColumns().addAll(usernameCol, firstNameCol, lastNameCol, roleCol);
            techTable.setItems(techData);
        }

        public static void saveAllTechnicianRoles(ObservableList<Technicians> techData) {
            String sql = "UPDATE technician SET role = ? WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                for (Technicians tech : techData) {
                    ps.setString(1, tech.getRole());
                    ps.setString(2, tech.getUserName());
                    ps.addBatch();
                }

                ps.executeBatch();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void loadVendorsTable(MFXTableView<Skeletons.Vendor> vendorsTable,
                                            ObservableList<Skeletons.Vendor> vendorData) {
            vendorsTable.getTableColumns().clear();

            vendorsTable.setTableRowFactory(item -> {
                MFXTableRow<Skeletons.Vendor> row = new MFXTableRow<>(vendorsTable, item);
                row.setPrefHeight(45);
                return row;
            });

            MFXTableColumn<Skeletons.Vendor> nameCol   = new MFXTableColumn<>("Vendor");
            MFXTableColumn<Skeletons.Vendor> labourCol = new MFXTableColumn<>("Pays Labour");
            MFXTableColumn<Skeletons.Vendor> partsCol  = new MFXTableColumn<>("Pays Parts");
            MFXTableColumn<Skeletons.Vendor> pstCol    = new MFXTableColumn<>("Pays PST");
            MFXTableColumn<Skeletons.Vendor> gstCol    = new MFXTableColumn<>("Pays GST");

            nameCol.setMinWidth(160);
            labourCol.setMinWidth(110);
            partsCol.setMinWidth(110);
            pstCol.setMinWidth(100);
            gstCol.setMinWidth(100);

            nameCol.setRowCellFactory(item ->
                    new MFXTableRowCell<>(Skeletons.Vendor::getName)
            );

            labourCol.setRowCellFactory(item -> {
                MFXTableRowCell<Skeletons.Vendor, Boolean> cell =
                        new MFXTableRowCell<>(Skeletons.Vendor::isPaysLabour);
                CheckBox cb = new CheckBox();
                cb.selectedProperty().bindBidirectional(item.paysLabourProperty());
                cell.setGraphic(cb);
                cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                return cell;
            });

            partsCol.setRowCellFactory(item -> {
                MFXTableRowCell<Skeletons.Vendor, Boolean> cell =
                        new MFXTableRowCell<>(Skeletons.Vendor::isPaysParts);
                CheckBox cb = new CheckBox();
                cb.selectedProperty().bindBidirectional(item.paysPartsProperty());
                cell.setGraphic(cb);
                cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                return cell;
            });

            pstCol.setRowCellFactory(item -> {
                MFXTableRowCell<Skeletons.Vendor, Boolean> cell =
                        new MFXTableRowCell<>(Skeletons.Vendor::isPaysPst);
                CheckBox cb = new CheckBox();
                cb.selectedProperty().bindBidirectional(item.paysPstProperty());
                cell.setGraphic(cb);
                cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                return cell;
            });

            gstCol.setRowCellFactory(item -> {
                MFXTableRowCell<Skeletons.Vendor, Boolean> cell =
                        new MFXTableRowCell<>(Skeletons.Vendor::isPaysGst);
                CheckBox cb = new CheckBox();
                cb.selectedProperty().bindBidirectional(item.paysGstProperty());
                cell.setGraphic(cb);
                cell.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                return cell;
            });

            vendorsTable.getTableColumns().addAll(nameCol, labourCol, partsCol, pstCol, gstCol);
            vendorsTable.setItems(vendorData);
            vendorsTable.setFooterVisible(false);
        }

        private static void openDescriptionPopup(WorkTable item, javafx.scene.Node anchor) {
            Stage popup = new Stage();
            popup.initStyle(StageStyle.TRANSPARENT);

            Window owner = anchor.getScene() != null ? anchor.getScene().getWindow() : null;
            if (owner != null) {
                popup.initOwner(owner);
                popup.initModality(Modality.WINDOW_MODAL);
            } else {
                popup.initModality(Modality.APPLICATION_MODAL);
            }

            String techName = item.getTech() != null && !item.getTech().isBlank() ? item.getTech() : "–";

            Label title = new Label("Repair Description");
            title.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold;");

            Label techBadge = new Label(techName);
            techBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.22);" +
                "-fx-text-fill: white;" +
                "-fx-padding: 3 12;" +
                "-fx-background-radius: 12;" +
                "-fx-font-size: 12;"
            );

            Region hSpacer = new Region();
            HBox.setHgrow(hSpacer, Priority.ALWAYS);
            HBox header = new HBox(10, title, hSpacer, techBadge);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle(
                "-fx-background-color: #0097A7;" +
                "-fx-padding: 14 20 14 20;" +
                "-fx-background-radius: 16 16 0 0;"
            );

            TextArea area = new TextArea(item.getDescription() != null ? item.getDescription() : "");
            area.setWrapText(true);
            area.setStyle(
                "-fx-font-size: 13;" +
                "-fx-background-color: #f8fafb;" +
                "-fx-border-color: #d8ecee;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;"
            );
            SpellCheckUtil.attach(area);
            VBox.setVgrow(area, Priority.ALWAYS);
            VBox.setMargin(area, new Insets(10, 16, 4, 16));

            Button cancelBtn = new Button("Cancel");
            cancelBtn.setStyle(
                "-fx-background-color: #f0f0f0;" +
                "-fx-text-fill: #444;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 7 20;" +
                "-fx-font-size: 12;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: transparent;"
            );

            Button doneBtn = new Button("Done");
            doneBtn.setStyle(
                "-fx-background-color: #0097A7;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 7 28;" +
                "-fx-font-size: 12;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: transparent;"
            );

            Region footerSpacer = new Region();
            HBox.setHgrow(footerSpacer, Priority.ALWAYS);
            HBox footer = new HBox(10, footerSpacer, cancelBtn, doneBtn);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setStyle(
                "-fx-background-color: white;" +
                "-fx-padding: 10 16 14 16;" +
                "-fx-background-radius: 0 0 16 16;"
            );

            VBox content = new VBox(0, header, area, footer);
            content.setPrefSize(680, 380);
            content.setMaxSize(680, 380);
            content.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 16;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 30, 0, 0, 8);"
            );

            StackPane root = new StackPane(content);
            root.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

            Scene scene = new Scene(root, 760, 460);
            scene.setFill(Color.TRANSPARENT);

            cancelBtn.setOnAction(ev -> popup.close());
            doneBtn.setOnAction(ev -> {
                item.setDescription(area.getText());
                popup.close();
            });
            scene.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ESCAPE) popup.close();
            });

            popup.setScene(scene);

            if (owner != null) {
                popup.setX(owner.getX() + owner.getWidth() / 2.0 - 380);
                popup.setY(owner.getY() + owner.getHeight() / 2.0 - 230);
            }

            popup.showAndWait();
        }
    }