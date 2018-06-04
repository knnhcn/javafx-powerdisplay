package ch.fhnw.cuie.project.template_simplecontrol.demo;

import ch.fhnw.cuie.project.template_simplecontrol.PowerDisplay;
import javafx.geometry.Insets;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

class DemoPane extends BorderPane {

    private final PresentationModel pm;

    // declare the custom control
    private PowerDisplay cc;

    // all controls
    private Slider      slider;
    private ColorPicker colorPicker;

    public DemoPane(PresentationModel pm) {
        this.pm = pm;
        initializeControls();
        layoutControls();
        setupBindings();
    }

    private void initializeControls() {
        setPadding(new Insets(10));

        cc = new PowerDisplay();

        slider = new Slider();
        slider.setShowTickLabels(true);

        pm.pmValueProperty().setValue(23);

        colorPicker = new ColorPicker();
    }

    private void layoutControls() {
        VBox controlPane = new VBox(new Label("PowerDisplay Properties"),
                                    slider, colorPicker);
        controlPane.setPadding(new Insets(0, 50, 0, 50));
        controlPane.setSpacing(10);

        setCenter(cc);
        setRight(controlPane);
    }

    private void setupBindings() {
        slider.valueProperty().bindBidirectional(pm.pmValueProperty());
        colorPicker.valueProperty().bindBidirectional(pm.baseColorProperty());

        cc.MAX_INPUT_VALUEProperty().bind(slider.maxProperty());
        cc.valueProperty().bindBidirectional(pm.pmValueProperty());
        cc.baseColorProperty().bindBidirectional(pm.baseColorProperty());
    }

}
