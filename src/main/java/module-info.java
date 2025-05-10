module main {
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires com.jfoenix;
    requires javafx.controls;

    opens main to javafx.fxml;
    exports main;
}
