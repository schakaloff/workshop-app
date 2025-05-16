module main {
    requires javafx.fxml;
    requires java.sql;
    requires com.jfoenix;
    requires javafx.controls;
    requires MaterialFX;

    opens main to javafx.fxml;
    exports main;
}
