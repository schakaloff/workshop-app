module main {
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires MaterialFX;
    requires org.apache.pdfbox;
    requires java.desktop;
    requires java.logging;

    opens main to javafx.fxml;
    exports main;
    exports Skeletons;
    opens Skeletons to javafx.fxml;
    exports DB;
    opens DB to javafx.fxml;
    exports Controllers;
    opens Controllers to javafx.fxml;
    exports utils;
    opens utils to javafx.fxml;
    exports utils.enums;
    opens utils.enums to javafx.fxml;
}