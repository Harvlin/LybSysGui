module LybSysGui {
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires java.sql;
    requires  mysql.connector.j;
    opens LibSysGui to javafx.fxml;
    exports LibSysGui;
}