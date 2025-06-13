module main {
    requires javafx.fxml;
    requires java.sql;
    requires com.jfoenix;
    requires javafx.controls;
    requires MaterialFX;
    requires org.apache.poi.ooxml;

    opens main to javafx.fxml;
    exports main;
    exports Skeletons;
    opens Skeletons to javafx.fxml;
    exports DB;
    opens DB to javafx.fxml;
    exports Controllers;
    opens Controllers to javafx.fxml;
}
