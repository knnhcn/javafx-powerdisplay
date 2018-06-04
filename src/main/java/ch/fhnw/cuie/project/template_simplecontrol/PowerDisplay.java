package ch.fhnw.cuie.project.template_simplecontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.scene.transform.Rotate;

/**
 * PowerDisplay zeigt mit dem Zeiger die MW an: je höher, desto mehr im roten Bereich.
 *
 * @author Kenan Hacan, Julien Christen
 */
public class PowerDisplay extends Region {
    // needed for StyleableProperties
    private static final StyleablePropertyFactory<PowerDisplay> FACTORY = new StyleablePropertyFactory<>(Region.getClassCssMetaData());

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return FACTORY.getCssMetaData();
    }

    private static final Locale CH = new Locale("de", "CH");

    private static final double ARTBOARD_WIDTH  = 200;  // Todo: Breite der "Zeichnung" aus dem Grafik-Tool übernehmen
    private static final double ARTBOARD_HEIGHT = 200;  // Todo: Anpassen an die Breite der Zeichnung

    private static final double ASPECT_RATIO = ARTBOARD_WIDTH / ARTBOARD_HEIGHT;

    private static final double MINIMUM_WIDTH  = 25;    // Todo: Anpassen
    private static final double MINIMUM_HEIGHT = MINIMUM_WIDTH / ASPECT_RATIO;

    private static final double MAXIMUM_WIDTH = 800;    // Todo: Anpassen

    // Todo: diese Parts durch alle notwendigen Parts der gewünschten CustomControl ersetzen
    private Circle frame;
    private Circle display;
    private Circle outerRing;
    private Circle innerRing;
    private Circle hubBelow;
    private Circle hubAbove;
    private Circle glass;

    private Circle shineLeftFull;
    private Circle antiShineLeft;

    private Circle shineRightFull;
    private Circle antiShineRight;

    private Shape shineLeft;
    private Shape shineRight;

    private Line pointer;

    private Shape ring;

    // Display colors
    private Arc green;
    private Arc lightgreen;
    private Arc yellow;
    private Arc orange;
    private Arc red;

    private Group arcGroup;
    private Circle arcInnerCircle;

    private List<Line> ticks;

    // Todo: ersetzen durch alle notwendigen Properties der CustomControl
    private final DoubleProperty value = new SimpleDoubleProperty();

    private final DoubleProperty MAX_INPUT_VALUE = new SimpleDoubleProperty();

    // Todo: ergänzen mit allen  CSS stylable properties
    private static final CssMetaData<PowerDisplay, Color> BASE_COLOR_META_DATA = FACTORY.createColorCssMetaData("-base-color", s -> s.baseColor);

    private final StyleableObjectProperty<Color> baseColor = new SimpleStyleableObjectProperty<Color>(BASE_COLOR_META_DATA, this, "baseColor") {
        @Override
        protected void invalidated() {
            setStyle(getCssMetaData().getProperty() + ": " + colorToCss(get()) + ";");
            applyCss();
        }
    };


    // needed for resizing
    private Pane drawingPane;

    public PowerDisplay() {
        initializeSelf();
        initializeParts();
        initializeDrawingPane();
        layoutParts();
        setupEventHandlers();
        setupValueChangeListeners();
        setupBindings();
    }

    private void initializeSelf() {
        // load stylesheets
        String fonts = getClass().getResource("/fonts/fonts.css").toExternalForm();
        getStylesheets().add(fonts);

        String stylesheet = getClass().getResource("style.css").toExternalForm();
        getStylesheets().add(stylesheet);

        getStyleClass().add("powerdisplay");  // Todo: an den Namen der Klasse (des CustomControls) anpassen
    }

    private void initializeParts() {
        //ToDo: alle deklarierten Parts initialisieren
        double center = ARTBOARD_WIDTH * 0.5;

        frame = new Circle(center, center, center);
        frame.getStyleClass().add("frame");

        outerRing = new Circle(95);
        innerRing = new Circle(89);
        ring = Shape.subtract(outerRing, innerRing);
        ring.getStyleClass().add("ring");

        display = new Circle(82.5);
        display.getStyleClass().add("display");

        pointer = new Line(34, 100, 115, 100);
        pointer.getTransforms().add(new Rotate(getDegreeFromPercent(valueProperty().doubleValue()), 100, 100));
        
        pointer.setStrokeWidth(5);
        pointer.setStrokeLineCap(StrokeLineCap.ROUND);
        pointer.getStyleClass().add("pointer");

        hubBelow = new Circle(17.5);
        hubBelow.getStyleClass().add("hub-below");

        hubAbove = new Circle(2.5);
        hubAbove.getStyleClass().add("hub-above");

        ticks = new ArrayList<>();

        int    numberOfTicks = 13;
        double overallAngle  = 180;
        double tickLength    = 10;
        double indent        = 20;
        double startingAngle = 90;

        double degreesBetweenTicks = overallAngle == 360 ?
                overallAngle /numberOfTicks :
                overallAngle /(numberOfTicks - 1);
        double outerRadius         = center - indent;
        double innerRadius         = center - indent - tickLength;

        for (int i = 0; i < numberOfTicks; i++) {
            double angle = 180 + startingAngle + i * degreesBetweenTicks;

            Point2D startPoint = pointOnCircle(center, center, outerRadius, angle);
            Point2D endPoint   = pointOnCircle(center, center, innerRadius, angle);

            Line tick = new Line(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
            tick.setStrokeWidth(2);
            tick.setStrokeLineCap(StrokeLineCap.ROUND);
            tick.getStyleClass().add("tick");
            ticks.add(tick);
        }

        green = new Arc();
        lightgreen = new Arc();
        yellow = new Arc();
        orange = new Arc();
        red = new Arc();

        arcInnerCircle = new Circle();
        arcInnerCircle.getStyleClass().add("arc-inner-circle");

        arcGroup = new Group();

        glass = new Circle(89);
        glass.getStyleClass().add("glass");

        shineLeftFull = new Circle(95);
        antiShineLeft = new Circle(95);

        shineRightFull = new Circle(89);
        antiShineRight = new Circle(89);

    }

    private void initializeDrawingPane() {
        drawingPane = new Pane();
        drawingPane.getStyleClass().add("drawing-pane");
        drawingPane.setMaxSize(ARTBOARD_WIDTH, ARTBOARD_HEIGHT);
        drawingPane.setMinSize(ARTBOARD_WIDTH, ARTBOARD_HEIGHT);
        drawingPane.setPrefSize(ARTBOARD_WIDTH, ARTBOARD_HEIGHT);
    }

    private void layoutParts() {
        frame.setCenterX(100);
        frame.setCenterY(100);
        display.setCenterX(100);
        display.setCenterY(100);
        outerRing.setCenterX(100);
        outerRing.setCenterY(100);
        innerRing.setCenterX(100);
        outerRing.setCenterY(100);

        arcInnerCircle.setCenterX(100);
        arcInnerCircle.setCenterY(100);

        hubBelow.setCenterX(100);
        hubBelow.setCenterX(100);
        hubBelow.setCenterY(100);

        hubAbove.setCenterX(100);
        hubAbove.setCenterX(100);
        hubAbove.setCenterY(100);

        ring.setLayoutX(100);
        ring.setLayoutY(100);

        // Set arcs on top of each others.
        green.setFill(Color.GREEN);
        green.setStroke(Color.GREEN);
        green.setCenterX(100);
        green.setCenterY(100);
        green.setRadiusX(68);
        green.setRadiusY(68);
        green.setStartAngle(-5);
        green.setLength(190);
        green.setType(ArcType.ROUND);

        lightgreen.setFill(Color.LIGHTGREEN);
        lightgreen.setStroke(Color.LIGHTGREEN);
        lightgreen.setCenterX(100);
        lightgreen.setCenterY(100);
        lightgreen.setRadiusX(68);
        lightgreen.setRadiusY(68);
        lightgreen.setStartAngle(0);
        lightgreen.setLength(150);
        lightgreen.setType(ArcType.ROUND);

        yellow.setFill(Color.YELLOW);
        yellow.setStroke(Color.YELLOW);
        yellow.setCenterX(100);
        yellow.setCenterY(100);
        yellow.setRadiusX(68);
        yellow.setRadiusY(68);
        yellow.setStartAngle(0);
        yellow.setLength(120);
        yellow.setType(ArcType.ROUND);

        orange.setFill(Color.ORANGE);
        orange.setStroke(Color.ORANGE);
        orange.setCenterX(100);
        orange.setCenterY(100);
        orange.setRadiusX(68);
        orange.setRadiusY(68);
        orange.setStartAngle(0);
        orange.setLength(60);
        orange.setType(ArcType.ROUND);

        red.setFill(Color.RED);
        red.setStroke(Color.RED);
        red.setCenterX(100);
        red.setCenterY(100);
        red.setRadiusX(68);
        red.setRadiusY(68);
        red.setStartAngle(-5);
        red.setLength(35);
        red.setType(ArcType.ROUND);

        arcInnerCircle.setRadius(35);
        arcInnerCircle.setStroke(Color.WHITE);
        arcInnerCircle.setFill(Color.WHITE);

        glass.setCenterX(100);
        glass.setCenterY(100);

        shineLeftFull.setCenterX(98);
        shineLeftFull.setCenterY(100);
        antiShineLeft.setCenterX(100);
        antiShineLeft.setCenterY(100);

        shineLeft = Shape.subtract(shineLeftFull, antiShineLeft);
        shineLeft.getStyleClass().add("shine-left");

        shineRightFull.setCenterX(100);
        shineRightFull.setCenterY(100);
        antiShineRight.setCenterX(98);
        antiShineRight.setCenterY(100);

        shineRight = Shape.subtract(shineRightFull, antiShineRight);
        shineRight.getStyleClass().add("shine-right");


        arcGroup.getChildren().addAll(green, lightgreen, yellow, orange, red);
        drawingPane.getChildren().addAll(frame, ring, display, arcGroup, arcInnerCircle);
        drawingPane.getChildren().addAll(ticks);
        drawingPane.getChildren().addAll(hubBelow, pointer, hubAbove, glass);
        drawingPane.getChildren().addAll(shineLeft, shineRight);

        getChildren().add(drawingPane);
    }

    private void setupEventHandlers() {
        //ToDo: bei Bedarf ergänzen
    }

    private void setupValueChangeListeners() {
        valueProperty().addListener((observable, oldValue, newValue) -> {
            double val = newValue.doubleValue();
            pointer.getTransforms().setAll(new Rotate(getDegreeFromPercent(val), 100, 100));
        });
    }

    private double getDegreeFromPercent(double n) {
        double percent = n / MAX_INPUT_VALUEProperty().doubleValue();
        double calc = 180 * percent;

        return calc;
    }

    private void setupBindings() {
        //ToDo dieses Binding ersetzen
    }


    //resize by scaling
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        resize();
    }

    private void resize() {
        Insets padding         = getPadding();
        double availableWidth  = getWidth() - padding.getLeft() - padding.getRight();
        double availableHeight = getHeight() - padding.getTop() - padding.getBottom();

        double width = Math.max(Math.min(Math.min(availableWidth, availableHeight * ASPECT_RATIO), MAXIMUM_WIDTH), MINIMUM_WIDTH);

        double scalingFactor = width / ARTBOARD_WIDTH;

        if (availableWidth > 0 && availableHeight > 0) {
            relocateCentered();
            drawingPane.setScaleX(scalingFactor);
            drawingPane.setScaleY(scalingFactor);
        }
    }

    private void relocateCentered() {
        drawingPane.relocate((getWidth() - ARTBOARD_WIDTH) * 0.5, (getHeight() - ARTBOARD_HEIGHT) * 0.5);
    }

    private void relocateCenterBottom(double scaleY, double paddingBottom) {
        double visualHeight = ARTBOARD_HEIGHT * scaleY;
        double visualSpace  = getHeight() - visualHeight;
        double y            = visualSpace + (visualHeight - ARTBOARD_HEIGHT) * 0.5 - paddingBottom;

        drawingPane.relocate((getWidth() - ARTBOARD_WIDTH) * 0.5, y);
    }

    private void relocateCenterTop(double scaleY, double paddingTop) {
        double visualHeight = ARTBOARD_HEIGHT * scaleY;
        double y            = (visualHeight - ARTBOARD_HEIGHT) * 0.5 + paddingTop;

        drawingPane.relocate((getWidth() - ARTBOARD_WIDTH) * 0.5, y);
    }

    // some handy functions

    //ToDo: diese Funktionen anschauen und für die Umsetzung des CustomControls benutzen

    private double percentageToValue(double percentage, double minValue, double maxValue){
        return ((maxValue - minValue) * percentage) + minValue;
    }

    private double valueToPercentage(double value, double minValue, double maxValue) {
        return (value - minValue) / (maxValue - minValue);
    }

    private double valueToAngle(double value, double minValue, double maxValue) {
        return percentageToAngle(valueToPercentage(value, minValue, maxValue));
    }

    private double mousePositionToValue(double mouseX, double mouseY, double cx, double cy, double minValue, double maxValue){
        double percentage = angleToPercentage(angle(cx, cy, mouseX, mouseY));

        return percentageToValue(percentage, minValue, maxValue);
    }

    private double angleToPercentage(double angle){
        return angle / 360.0;
    }

    private double percentageToAngle(double percentage){
        return 360.0 * percentage;
    }

    private double angle(double cx, double cy, double x, double y) {
        double deltaX = x - cx;
        double deltaY = y - cy;
        double radius = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        double nx     = deltaX / radius;
        double ny     = deltaY / radius;
        double theta  = Math.toRadians(90) + Math.atan2(ny, nx);

        return Double.compare(theta, 0.0) >= 0 ? Math.toDegrees(theta) : Math.toDegrees((theta)) + 360.0;
    }

    private Point2D pointOnCircle(double cX, double cY, double radius, double angle) {
        return new Point2D(cX - (radius * Math.sin(Math.toRadians(angle - 180))),
                           cY + (radius * Math.cos(Math.toRadians(angle - 180))));
    }

    private Text createCenteredText(String styleClass) {
        return createCenteredText(ARTBOARD_WIDTH * 0.5, ARTBOARD_HEIGHT * 0.5, styleClass);
    }

    private Text createCenteredText(double cx, double cy, String styleClass) {
        Text text = new Text();
        text.getStyleClass().add(styleClass);
        text.setTextOrigin(VPos.CENTER);
        text.setTextAlignment(TextAlignment.CENTER);
        double width = cx > ARTBOARD_WIDTH * 0.5 ? ((ARTBOARD_WIDTH - cx) * 2.0) : cx * 2.0;
        text.setWrappingWidth(width);
        text.setBoundsType(TextBoundsType.VISUAL);
        text.setY(cy);
        text.setX(cx - (width / 2.0));

        return text;
    }
//
//    private Group createTicks(double cx, double cy, int numberOfTicks, double overallAngle, double tickLength, double indent, double startingAngle, String styleClass) {
//        Group group = new Group();
//
//        double degreesBetweenTicks = overallAngle == 360 ?
//                                     overallAngle /numberOfTicks :
//                                     overallAngle /(numberOfTicks - 1);
//        double outerRadius         = Math.min(cx, cy) - indent;
//        double innerRadius         = Math.min(cx, cy) - indent - tickLength;
//
//        for (int i = 0; i < numberOfTicks; i++) {
//            double angle = 180 + startingAngle + i * degreesBetweenTicks;
//
//            Point2D startPoint = pointOnCircle(cx, cy, outerRadius, angle);
//            Point2D endPoint   = pointOnCircle(cx, cy, innerRadius, angle);
//
//            Line tick = new Line(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
//            tick.getStyleClass().add(styleClass);
//            group.getChildren().add(tick);
//        }
//
//        return group;
//    }
//
//    private Group createNumberedTicks(double cx, double cy, int numberOfTicks, double overallAngle, double tickLength, double indent, double startingAngle, String styleClass) {
//            Group group = new Group();
//
//            int width = 30;
//            double degreesBetweenTicks = overallAngle == 360 ?
//                    overallAngle / numberOfTicks :
//                    overallAngle / (numberOfTicks - 1);
//            double outerRadius = Math.min(cx - width, cy - width) - indent;
//            double innerRadius = Math.min(cx - width, cy - width) - indent - tickLength;
//
//            for (int i = 0; i < numberOfTicks; i++) {
//                double angle = 180 + startingAngle + i * degreesBetweenTicks;
//
//                if (i % 5 == 0 && i % 2 != 0) {
//                    Point2D startPoint = pointOnCircle(cx, cy, outerRadius, angle);
//                    Point2D endPoint = pointOnCircle(cx, cy, innerRadius * 0.95, angle);
//                    Line tick = new Line(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
//                    tick.getStyleClass().add(styleClass);
//                    group.getChildren().add(tick);
//                }
//                if (i % 10 == 0) {
//                    Point2D startPoint = pointOnCircle(cx, cy, outerRadius, angle);
//                    Point2D endPoint = pointOnCircle(cx, cy, innerRadius * 0.9, angle);
//
//                    Line bigTick = new Line(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
//                    bigTick.getStyleClass().add(styleClass);
//                    group.getChildren().add(bigTick);
//
//                    Point2D textPosition = pointOnCircle(cx - 7, cy + 5, innerRadius * 0.8, angle);
//                    Text number = new Text(textPosition.getX(), textPosition.getY(), Integer.toString(i));
//                    number.getStyleClass().add("tick-number");
//                    group.getChildren().add(number);
//
//                } else {
//                    Point2D startPoint = pointOnCircle(cx, cy, outerRadius, angle);
//                    Point2D endPoint = pointOnCircle(cx, cy, innerRadius, angle);
//                    Line tick = new Line(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
//                    tick.getStyleClass().add(styleClass);
//                    group.getChildren().add(tick);
//                }
//            }
//
//            return group;
//        }

    private String colorToCss(final Color color) {
  		return color.toString().replace("0x", "#");
  	}


    // compute sizes

    @Override
    protected double computeMinWidth(double height) {
        Insets padding           = getPadding();
        double horizontalPadding = padding.getLeft() + padding.getRight();

        return MINIMUM_WIDTH + horizontalPadding;
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets padding         = getPadding();
        double verticalPadding = padding.getTop() + padding.getBottom();

        return MINIMUM_HEIGHT + verticalPadding;
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets padding           = getPadding();
        double horizontalPadding = padding.getLeft() + padding.getRight();

        return ARTBOARD_WIDTH + horizontalPadding;
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets padding         = getPadding();
        double verticalPadding = padding.getTop() + padding.getBottom();

        return ARTBOARD_HEIGHT + verticalPadding;
    }

    // alle getter und setter  (generiert via "Code -> Generate... -> Getter and Setter)

    // ToDo: ersetzen durch die Getter und Setter Ihres CustomControls
    public double getValue() {
        return value.get();
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    public void setValue(double value) {
        this.value.set(value);
    }

    public Color getBaseColor() {
        return baseColor.get();
    }

    public StyleableObjectProperty<Color> baseColorProperty() {
        return baseColor;
    }

    public void setBaseColor(Color baseColor) {
        this.baseColor.set(baseColor);
    }

    public double getMAX_INPUT_VALUE() {
        return MAX_INPUT_VALUE.get();
    }

    public DoubleProperty MAX_INPUT_VALUEProperty() {
        return MAX_INPUT_VALUE;
    }

    public void setMAX_INPUT_VALUE(double MAX_INPUT_VALUE) {
        this.MAX_INPUT_VALUE.set(MAX_INPUT_VALUE);
    }
}
