module codes.acegym {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires javafx.graphics;
    requires javafx.base;
    requires java.prefs;
    requires jakarta.mail;

    opens codes.acegym.Objects to javafx.base, javafx.fxml;

    opens codes.acegym to javafx.fxml;
    exports codes.acegym.Controllers;
    opens codes.acegym.Controllers to javafx.fxml;
    exports codes.acegym.Application_Launcher;
    opens codes.acegym.Application_Launcher to javafx.fxml;
    exports codes.acegym.DB;
    exports codes.acegym.Objects;
    opens codes.acegym.DB to javafx.fxml;
}