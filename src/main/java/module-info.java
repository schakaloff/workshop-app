module main {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens main to javafx.fxml;
    exports main;
}
