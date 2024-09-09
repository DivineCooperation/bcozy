package org.openbase.bco.bcozy.view.generic;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;
import org.openbase.jul.iface.provider.LabelProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.visual.javafx.geometry.svg.SVGGlyphIcon;
import org.openbase.jul.visual.javafx.iface.DynamicPane;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;

import static org.openbase.bco.dal.lib.layer.service.provider.EmphasisStateProviderService.*;

public class EmphasisControlTrianglePane extends BorderPane implements DynamicPane {

    private final SimpleObjectProperty<EmphasisState> emphasisStateProperty;
    private final SimpleObjectProperty<MouseEvent> interactionProperty = new SimpleObjectProperty<>();

    private final EmphasisControlTriangle emphasisControlTriangle;
    private final Canvas canvas;
    private final Label emphasisLabel = new Label("Emphasis");
    private final SVGGlyphIcon emphasisIcon;
    private final HBox triangleOuterHPane;
    private GraphicsContext gc;
    private double scale;
    private double xTranslate;
    private double yTranslate;
    private final Pane trianglePane;
    private volatile boolean emphasisStateUpdate;

    private EmphasisDescriptionProvider labelProvider = primaryEmphasisCategory -> "Optimize " + StringProcessor.transformFirstCharToUpperCase(primaryEmphasisCategory.name().toLowerCase());

    public EmphasisControlTrianglePane() {

        this.emphasisStateProperty = new SimpleObjectProperty<>(EmphasisState.getDefaultInstance());
        this.emphasisControlTriangle = new EmphasisControlTriangle();
        this.emphasisStateUpdate = false;
        this.emphasisControlTriangle.primaryEmphasisCategoryProperty().addListener((observable, oldValue, primaryEmphasisCategory) -> {
            updateEmphasisCategory(primaryEmphasisCategory);
        });

        this.emphasisControlTriangle.setEmphasisStateChangeListener(() -> {
            computeEmphasisState();
        });

        this.emphasisControlTriangle.setHandlePositionChangeListener(() -> {
            updateIcon(false);
        });

        this.emphasisStateProperty.addListener((observable, oldValue, newValue) -> {

            // only update if update is not already in progress
            if (emphasisStateUpdate) {
                return;
            }

            emphasisControlTriangle.updateEmphasisState(newValue.getComfort(), newValue.getEconomy(), newValue.getSecurity(), false, gc);
        });

        this.emphasisIcon = new SVGGlyphIcon(MaterialDesignIcon.HELP, 0, false);
        this.emphasisIcon.setMouseTransparent(true);

        this.canvas = new Canvas();
        this.trianglePane = new Pane();

        this.gc = canvas.getGraphicsContext2D();

        this.trianglePane.setPrefSize(200, 185);

        this.canvas.setCache(true);

        canvas.widthProperty().bind(trianglePane.widthProperty());
        canvas.heightProperty().bind(trianglePane.heightProperty());

        this.trianglePane.heightProperty().addListener((observable, oldValue, newValue) -> {
            this.updateDynamicContent();
        });

        this.trianglePane.widthProperty().addListener((observable, oldValue, newValue) -> {
            this.updateDynamicContent();
        });

        this.canvas.setOnMouseDragged(event -> {
            applyMousePositionUpdate(event.getX(), event.getY(), scale, true, gc);
            event.consume();
        });

        this.canvas.setOnMouseDragReleased(event -> {
            applyMousePositionUpdate(event.getX(), event.getY(), scale, false, gc);
            event.consume();
        });

        this.canvas.setOnMousePressed(event -> {
            applyMousePositionUpdate(event.getX(), event.getY(), scale, true, gc);
            event.consume();
            interactionProperty.set(event);
        });

        this.canvas.setOnMouseClicked(event -> {
            applyMousePositionUpdate(event.getX(), event.getY(), scale, false, gc);
            event.consume();
        });

        this.canvas.setOnMouseReleased(event -> {
            applyMousePositionUpdate(event.getX(), event.getY(), scale, false, gc);
            event.consume();
            interactionProperty.set(event);
        });

        this.trianglePane.getChildren().addAll(canvas, emphasisIcon);
        AnchorPane.setTopAnchor(trianglePane, 0.0);
        AnchorPane.setBottomAnchor(trianglePane, 0.0);
        AnchorPane.setLeftAnchor(trianglePane, 0.0);
        AnchorPane.setRightAnchor(trianglePane, 0.0);

        final VBox triangleOuterVPane = new VBox();
        triangleOuterVPane.setAlignment(Pos.CENTER);
        triangleOuterVPane.setFillWidth(true);
        triangleOuterVPane.getChildren().add(trianglePane);

        triangleOuterHPane = new HBox();
        triangleOuterHPane.setAlignment(Pos.CENTER);
        triangleOuterHPane.setFillHeight(true);
        triangleOuterHPane.getChildren().add(triangleOuterVPane);

        trianglePane.prefHeightProperty().bind(triangleOuterHPane.heightProperty());
        trianglePane.prefWidthProperty().bind(triangleOuterHPane.widthProperty());

        emphasisLabel.setTextAlignment(TextAlignment.CENTER);
        emphasisLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        final HBox labelBox = new HBox(emphasisLabel);
        labelBox.setAlignment(Pos.CENTER);

        this.setCenter(triangleOuterHPane);
        this.setBottom(labelBox);

        this.initContent();
        this.updateDynamicContent();
    }


    @Override
    public void initContent() {
        updateEmphasisCategory(emphasisControlTriangle.primaryEmphasisCategoryProperty().getValue());
    }

    private void updateEmphasisCategory(Category primaryEmphasisCategory) {

        if(Platform.isFxApplicationThread()) {
            switch (primaryEmphasisCategory) {
                case ECONOMY:
                    emphasisIcon.setForegroundIcon(MaterialDesignIcon.LEAF);
                    break;
                case COMFORT:
                    emphasisIcon.setForegroundIcon(MaterialDesignIcon.EMOTICON);
                    break;
                case SECURITY:
                    emphasisIcon.setForegroundIcon(MaterialDesignIcon.SECURITY);
                    break;
                case UNKNOWN:
                    emphasisIcon.setForegroundIcon(MaterialDesignIcon.FLASH);
                    break;
            }
            emphasisLabel.setText(labelProvider.getLabel(primaryEmphasisCategory));
        } else {
            Platform.runLater(() -> {
                switch (primaryEmphasisCategory) {
                    case ECONOMY:
                        emphasisIcon.setForegroundIcon(MaterialDesignIcon.LEAF);
                        break;
                    case COMFORT:
                        emphasisIcon.setForegroundIcon(MaterialDesignIcon.EMOTICON);
                        break;
                    case SECURITY:
                        emphasisIcon.setForegroundIcon(MaterialDesignIcon.SECURITY);
                        break;
                    case UNKNOWN:
                        emphasisIcon.setForegroundIcon(MaterialDesignIcon.FLASH);
                        break;
                }
                emphasisLabel.setText(labelProvider.getLabel(primaryEmphasisCategory));
            });
        }
    }

    public EmphasisControlTriangle getEmphasisControlTriangle() {
        return emphasisControlTriangle;
    }

    private void computeEmphasisState() {

        final EmphasisState.Builder emphasisState = EmphasisState.newBuilder();
        emphasisState
                .setComfort(emphasisControlTriangle.getComfort())
                .setSecurity(emphasisControlTriangle.getSecurity())
                .setEconomy(emphasisControlTriangle.getEconomy());
        emphasisStateUpdate = true;
        emphasisStateProperty.setValue(emphasisState.build());
        emphasisStateUpdate = false;
    }

    @Override
    public void updateDynamicContent() {

        scale = Math.min(canvas.getWidth(), canvas.getHeight()) / (EMPHASIS_TRIANGLE_OUTER_LINE + EmphasisControlTriangle.PADDING * 2);
//        System.out.println("canvas.getWidth(): " + canvas.getWidth());
//        System.out.println("canvas.getHeight(): " + canvas.getHeight());
//        System.out.println("getHeight(): " + getHeight());
//        System.out.println("getWidth: " + getWidth());
//        System.out.println("scale: " + scale);
//        System.out.println("triangle size: " + EMPHASIS_TRIANGLE_OUTER_LINE);
//        System.out.println("triangle size scaled: " + (EMPHASIS_TRIANGLE_OUTER_LINE * scale));
//        System.out.println("triangle leftover: " + ((canvas.getWidth() - EMPHASIS_TRIANGLE_OUTER_LINE * scale)/2));

        xTranslate = ((canvas.getWidth()/scale - EMPHASIS_TRIANGLE_OUTER_LINE)/2);
        yTranslate = ((canvas.getHeight()/scale - EMPHASIS_TRIANGLE_HEIGHT)/2);

        // reset previous scale value
        gc.setTransform(new Affine());

        // set new scale
        gc.scale(scale, scale);

        // translate into center of canvas
        gc.translate(xTranslate, yTranslate);

        // initial triangle draw
        emphasisControlTriangle.drawShapes(false, gc);

        //gc.strokeRect(- EmphasisControlTriangle.PADDING, - EmphasisControlTriangle.PADDING, EMPHASIS_TRIANGLE_OUTER_LINE + EmphasisControlTriangle.PADDING * 2, EMPHASIS_TRIANGLE_OUTER_LINE + EmphasisControlTriangle.PADDING * 2);

        // setup initial icon position
        emphasisIcon.setSize(EmphasisControlTriangle.HANDLE_INNER_SIZE * scale * 0.80);

        // update icon pos
        updateIcon(false);

        // required to update icon position
        updateEmphasisCategory(emphasisControlTriangle.getPrimaryEmphasisCategory());

        requestLayout();
    }

    private void applyMousePositionUpdate(final double sceneX, final double sceneY, final double scale, final boolean mouseClicked, final GraphicsContext gc) {
        emphasisControlTriangle.updateHandlePosition(sceneX, sceneY, scale, xTranslate, yTranslate, mouseClicked, gc);
    }

    private void updateIcon(final boolean mouseClicked) {
        // setup icon animation
        if (!mouseClicked) {
            emphasisIcon.setForegroundIconColorAnimated(emphasisControlTriangle.getEmphasisColor(), 2);
        }

        emphasisIcon.setLayoutX((emphasisControlTriangle.getHandlePosX() + xTranslate) * scale - (emphasisIcon.getSize() / 2) - EmphasisControlTriangle.PADDING * scale);
        emphasisIcon.setLayoutY((emphasisControlTriangle.getHandlePosY() + yTranslate) * scale - (emphasisIcon.getSize() / 2) - EmphasisControlTriangle.PADDING * scale);
    }


    public SimpleObjectProperty<EmphasisState> emphasisStateProperty() {
        return emphasisStateProperty;
    }

    public SimpleDoubleProperty economyProperty() {
        return emphasisControlTriangle.economyProperty();
    }

    public SimpleDoubleProperty securityProperty() {
        return emphasisControlTriangle.securityProperty();
    }

    public SimpleDoubleProperty comfortProperty() {
        return emphasisControlTriangle.comfortProperty();
    }

    public SimpleObjectProperty<MouseEvent> interactionProperty() {
        return interactionProperty;
    }

    public void setTrianglePrefSize(double prefWidth, double prefHeight) {
        triangleOuterHPane.setPrefSize(prefWidth, prefHeight);
    }

    public void setLabelProvider(EmphasisDescriptionProvider labelProvider) {
        this.labelProvider = labelProvider;
    }
}
