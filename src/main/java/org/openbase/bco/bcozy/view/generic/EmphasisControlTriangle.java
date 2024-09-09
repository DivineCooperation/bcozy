package org.openbase.bco.bcozy.view.generic;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.ChangeListener;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point2d;

import static org.openbase.bco.dal.lib.layer.service.provider.EmphasisStateProviderService.*;
import static org.openbase.bco.dal.lib.layer.service.provider.EmphasisStateProviderService.EMPHASIS_TRIANGLE_HEIGHT;

public class EmphasisControlTriangle extends Triangle {

    private Logger LOGGER = LoggerFactory.getLogger(EmphasisControlTriangle.class);

    private double handlePosX = EMPHASIS_TRIANGLE_OUTER_LINE_HALF;
    private double handlePosY = EMPHASIS_TRIANGLE_HEIGHT_HALF;

    public static double BRIGHTNESS = 0.8d;
    public static double SATURATION = 0.7d;

    public static final double SECURITY_HUE = 222;
    public static final double ECONOMY_HUE = 149;
    public static final double COMFORT_HUE = 270;
    public static final double ERROR_HUE = 0.7d;

    private Color emphasisColor;

    public static final double EMPHASIS_TRIANGLE_INNER_LINE_STROKE = 0.03d;
    public static final double EMPHASIS_TRIANGLE_OUTER_LINE_STROKE = 0.05d;

    public static final double HANDLE_STROKE = 0.03d;
    public static final double HANDLE_SIZE = 0.3d;
    public static final double HANDLE_SIZE_HALF = HANDLE_SIZE / 2;
    public static final double HANDLE_INNER_SIZE = HANDLE_SIZE * 0.6;
    public static final double HANDLE_INNER_SIZE_HALF = HANDLE_INNER_SIZE / 2;

    public static final double PADDING = HANDLE_SIZE_HALF + HANDLE_STROKE;

    public static final double EMPHASIS_TRIANGLE_HEIGHT_RATIO = (EMPHASIS_TRIANGLE_OUTER_LINE + PADDING * 2) / (EMPHASIS_TRIANGLE_HEIGHT + PADDING * 2);
    public static final double EMPHASIS_TRIANGLE_WIDTH_RATIO = (EMPHASIS_TRIANGLE_HEIGHT + PADDING * 2) / (EMPHASIS_TRIANGLE_OUTER_LINE + PADDING * 2);


    private final double[] mainTriangleX = new double[3];
    private final double[] mainTriangleY = new double[3];
    private final double[] economyTriangleX = new double[3];
    private final double[] economyTriangleY = new double[3];
    private final double[] comfortTriangleX = new double[3];
    private final double[] comfortTriangleY = new double[3];
    private final double[] securityTriangleX = new double[3];
    private final double[] securityTriangleY = new double[3];

    private SimpleDoubleProperty comfortProperty = new SimpleDoubleProperty(computeComfortTriangleArea(handlePosX, (EMPHASIS_TRIANGLE_HEIGHT - handlePosY)));
    private SimpleDoubleProperty economyProperty = new SimpleDoubleProperty(computeEconomyTriangleArea(handlePosX, (EMPHASIS_TRIANGLE_HEIGHT - handlePosY)));
    private SimpleDoubleProperty securityProperty = new SimpleDoubleProperty(computeSecurityTriangleArea(handlePosX, (EMPHASIS_TRIANGLE_HEIGHT - handlePosY)));
    private SimpleObjectProperty<Category> primaryEmphasisCategoryProperty = new SimpleObjectProperty<>(Category.UNKNOWN);

    private double securityBrightness = Math.max(0d, Math.min(BRIGHTNESS, securityProperty.get() * 2));
    private double economyBrightness = Math.max(0d, Math.min(BRIGHTNESS, economyProperty.get() * 2));
    private double comfortBrightness = Math.max(0d, Math.min(BRIGHTNESS, comfortProperty.get() * 2));

    private ChangeListener emphasisStateChangeListener;
    private ChangeListener handlePositionChangeListener;

    public EmphasisControlTriangle() {
        super(0, 0, EMPHASIS_TRIANGLE_OUTER_LINE_HALF, EMPHASIS_TRIANGLE_HEIGHT, EMPHASIS_TRIANGLE_OUTER_LINE, 0);

        // set outer main triangle
        this.mainTriangleX[0] = 0;
        this.mainTriangleY[0] = EMPHASIS_TRIANGLE_HEIGHT;
        this.mainTriangleX[1] = EMPHASIS_TRIANGLE_OUTER_LINE_HALF;
        this.mainTriangleY[1] = 0;
        this.mainTriangleX[2] = EMPHASIS_TRIANGLE_OUTER_LINE;
        this.mainTriangleY[2] = EMPHASIS_TRIANGLE_HEIGHT;

        // set inner comfort triangle
        this.comfortTriangleX[0] = 0;
        this.comfortTriangleY[0] = EMPHASIS_TRIANGLE_HEIGHT;
        this.comfortTriangleX[1] = EMPHASIS_TRIANGLE_OUTER_LINE_HALF;
        this.comfortTriangleY[1] = 0;
        this.comfortTriangleX[2] = handlePosX;
        this.comfortTriangleY[2] = handlePosY;

        // set inner economy triangle
        this.economyTriangleX[0] = handlePosX;
        this.economyTriangleY[0] = handlePosY;
        this.economyTriangleX[1] = EMPHASIS_TRIANGLE_OUTER_LINE_HALF;
        this.economyTriangleY[1] = 0;
        this.economyTriangleX[2] = EMPHASIS_TRIANGLE_OUTER_LINE;
        this.economyTriangleY[2] = EMPHASIS_TRIANGLE_HEIGHT;

        // set inner security triangle
        this.securityTriangleX[0] = 0;
        this.securityTriangleY[0] = EMPHASIS_TRIANGLE_HEIGHT;
        this.securityTriangleX[1] = handlePosX;
        this.securityTriangleY[1] = handlePosY;
        this.securityTriangleX[2] = EMPHASIS_TRIANGLE_OUTER_LINE;
        this.securityTriangleY[2] = EMPHASIS_TRIANGLE_HEIGHT;

        this.updateEmphasisCategory();
    }

    double mousePosX;
    double mousePosY;

    public void updateHandlePosition(final double sceneMousePosX, final double sceneMousePosY, final double scale, final double xTranslate, final double yTranslate, final boolean mouseClicked, final GraphicsContext gc) {
        mousePosX = (sceneMousePosX) / scale - xTranslate;
        mousePosY = (sceneMousePosY) / scale - yTranslate;

        // compute handle position
        if (contains(mousePosX, (EMPHASIS_TRIANGLE_HEIGHT - mousePosY))) {
            // mouse is within the triangle, therefore just use mouse position for handle.
            handlePosX = mousePosX;
            handlePosY = mousePosY;
        } else {
            // mouse is not within the triangle, therefore we need to compute the triangle border intersection and use those position for the handle

            // compute bottom triangle intersection
            final Point2d mousePos = new Point2d(mousePosX, mousePosY);
            double bottomDistance;
            double leftDistance;

            try {
                final Point2d bottomTriangleCorner = calculateTriangleOuterBoundsIntersection(mainTriangleX[0], mainTriangleY[0], mainTriangleX[2], mainTriangleY[2], mainTriangleX[1], mainTriangleY[1], mousePosX, mousePosY, true);
                bottomDistance = mousePos.distance(bottomTriangleCorner);
                handlePosX = bottomTriangleCorner.x;
                handlePosY = bottomTriangleCorner.y;
            } catch (NotAvailableException ex) {
                bottomDistance = Double.MAX_VALUE;
                ExceptionPrinter.printHistory(ex, LOGGER);
            }

            try {
                final Point2d leftTriangleCorner = calculateTriangleOuterBoundsIntersection(mainTriangleX[1], mainTriangleY[1], mainTriangleX[0], mainTriangleY[0], mainTriangleX[2], mainTriangleY[2], mousePosX, mousePosY, true);
                leftDistance = mousePos.distance(leftTriangleCorner);
                if (leftDistance < bottomDistance) {
                    handlePosX = leftTriangleCorner.x;
                    handlePosY = leftTriangleCorner.y;
                }
            } catch (NotAvailableException ex) {
                leftDistance = Double.MAX_VALUE;
                ExceptionPrinter.printHistory(ex, LOGGER);
            }

            try {
                final Point2d rightTriangleCorner = calculateTriangleOuterBoundsIntersection(mainTriangleX[1], mainTriangleY[1], mainTriangleX[2], mainTriangleY[2], mainTriangleX[0], mainTriangleY[0], mousePosX, mousePosY, true);
                if (mousePos.distance(rightTriangleCorner) < Math.min(bottomDistance, leftDistance)) {
                    handlePosX = rightTriangleCorner.x;
                    handlePosY = rightTriangleCorner.y;
                }
            } catch (NotAvailableException ex) {
                // no nothing
                ExceptionPrinter.printHistory(ex, LOGGER);
            }
        }

        //System.out.println("x:" + x);
        //System.out.println("y:" + (EMPHASIS_TRIANGLE_HEIGHT - y));

        updateTriangleShape(handlePosX, handlePosY);

        // compute values
        updateEmphasisState(
                computeComfortTriangleArea(handlePosX, (EMPHASIS_TRIANGLE_HEIGHT - handlePosY)),
                computeEconomyTriangleArea(handlePosX, (EMPHASIS_TRIANGLE_HEIGHT - handlePosY)),
                computeSecurityTriangleArea(handlePosX, (EMPHASIS_TRIANGLE_HEIGHT - handlePosY)),
                mouseClicked,
                false,
                gc
        );

        // inform about emphasis state update
        if (emphasisStateChangeListener != null) {
            try {
                emphasisStateChangeListener.notifyChange();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not inform listener about emphasis state update!", ex, LOGGER);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setEmphasisStateChangeListener(final ChangeListener changeListener) {
        this.emphasisStateChangeListener = changeListener;
    }

    public void setHandlePositionChangeListener(final ChangeListener changeListener) {
        this.handlePositionChangeListener = changeListener;
    }

    private void updateTriangleShape(final double handlePosX, final double handlePosY) {
        comfortTriangleX[2] = handlePosX;
        comfortTriangleY[2] = handlePosY;
        economyTriangleX[0] = handlePosX;
        economyTriangleY[0] = handlePosY;
        securityTriangleX[1] = handlePosX;
        securityTriangleY[1] = handlePosY;

        // inform about handle position update
        if (handlePositionChangeListener != null) {
            try {
                handlePositionChangeListener.notifyChange();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not inform listener about handle update!", ex, LOGGER);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void updateEmphasisState(final double comfort, final double economy, final double security, final boolean mouseClicked, final GraphicsContext gc) {
        updateEmphasisState(comfort, economy, security, mouseClicked, true, gc);
    }

    public void updateEmphasisState(final double comfort, final double economy, final double security, final boolean mouseClicked, final boolean updateHandlePos, final GraphicsContext gc) {

        // filter non changing updates when mouse is clicked
        if(mouseClicked) {
            if (comfort == comfortProperty.get() && economy == economyProperty.get() && security == securityProperty.get()) {
                //System.out.println("skip non changing update: c:"+ comfort + " e:" + economy + " s:"+security);
                return;
            }
        }

        //System.out.println("update emphasis: c:"+ comfort + " e:" + economy + " s:"+security);
        comfortProperty.set(comfort);
        economyProperty.set(economy);
        securityProperty.set(security);

        securityBrightness = Math.max(0d, Math.min(BRIGHTNESS, securityProperty.get() * 2));
        economyBrightness = Math.max(0d, Math.min(BRIGHTNESS, economyProperty.get() * 2));
        comfortBrightness = Math.max(0d, Math.min(BRIGHTNESS, comfortProperty.get() * 2));

        // update handle pos if required
        if (updateHandlePos) {
            final Point2d handlePosition = computeTriangleHandlePosition(EmphasisState.newBuilder().setComfort(comfortProperty.get()).setEconomy(economyProperty.get()).setSecurity(securityProperty.get()).build());
            handlePosX = handlePosition.x;
            handlePosY = EMPHASIS_TRIANGLE_HEIGHT - handlePosition.y;
            updateTriangleShape(handlePosX, handlePosY);
        }

        // update emphasis category and triangle color.
        updateEmphasisCategory();

        // draw final shape
        drawShapes(mouseClicked, gc);
    }

    private void updateEmphasisCategory() {
        if (securityProperty.get() > economyProperty.get() && securityProperty.get() > comfortProperty.get()) {
            primaryEmphasisCategoryProperty.set(Category.SECURITY);
            emphasisColor = Color.hsb(SECURITY_HUE, SATURATION, BRIGHTNESS);
        } else if (comfortProperty.get() > economyProperty.get() && comfortProperty.get() > securityProperty.get()) {
            primaryEmphasisCategoryProperty.set(Category.COMFORT);
            emphasisColor = Color.hsb(COMFORT_HUE, SATURATION, BRIGHTNESS);
        } else if (economyProperty.get() > comfortProperty.get() && economyProperty.get() > securityProperty.get()) {
            primaryEmphasisCategoryProperty.set(Category.ECONOMY);
            emphasisColor = Color.hsb(ECONOMY_HUE, SATURATION, BRIGHTNESS);
        } else {
            primaryEmphasisCategoryProperty.set(Category.UNKNOWN);
            // error
            emphasisColor = Color.hsb(ERROR_HUE, SATURATION, BRIGHTNESS);
        }
    }

    public Color getEmphasisColor() {
        return emphasisColor;
    }

    public void drawShapes(final boolean mouseClicked, final GraphicsContext gc) {

        // clear canvas
        gc.clearRect(-PADDING, -PADDING, gc.getCanvas().getWidth() + PADDING, gc.getCanvas().getHeight() + PADDING);

        // main triangle
        // =============

        // format
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(EMPHASIS_TRIANGLE_INNER_LINE_STROKE);


        // =============
        // format
        gc.setFill(Color.hsb(ECONOMY_HUE, SATURATION, economyBrightness));
        // paint
        gc.fillPolygon(economyTriangleX, economyTriangleY, 3);

        // comfort triangle
        // =============
        // format
        gc.setFill(Color.hsb(COMFORT_HUE, SATURATION, comfortBrightness));
        // paint
        gc.fillPolygon(comfortTriangleX, comfortTriangleY, 3);

        // security triangle
        // =============
        // format
        gc.setFill(Color.hsb(SECURITY_HUE, SATURATION, securityBrightness));
        // paint
        gc.fillPolygon(securityTriangleX, securityTriangleY, 3);

        // paint frames
//        gc.strokePolygon(economyTriangleX, economyTriangleY, 3);
//        gc.strokePolygon(comfortTriangleX, comfortTriangleY, 3);
//        gc.strokePolygon(securityTriangleX, securityTriangleY, 3);

        gc.strokeLine(comfortTriangleX[0], comfortTriangleY[0], comfortTriangleX[2], comfortTriangleY[2]);
        gc.strokeLine(economyTriangleX[0], economyTriangleY[0], economyTriangleX[1], economyTriangleY[1]);
        gc.strokeLine(securityTriangleX[2], securityTriangleY[2], securityTriangleX[1], securityTriangleY[1]);

        gc.setLineWidth(EMPHASIS_TRIANGLE_OUTER_LINE_STROKE);
        gc.strokePolygon(mainTriangleX, mainTriangleY, 3);

        // draw handle
        gc.setLineWidth(HANDLE_STROKE);
        gc.setStroke(Color.BLACK);

        if (mouseClicked) {
            gc.setFill(Color.GRAY);
        } else {
            gc.setFill(Color.WHITE);
        }

        gc.fillOval(handlePosX - HANDLE_SIZE_HALF, handlePosY - HANDLE_SIZE_HALF, HANDLE_SIZE, HANDLE_SIZE);
        gc.strokeOval(handlePosX - HANDLE_SIZE_HALF, handlePosY - HANDLE_SIZE_HALF, HANDLE_SIZE, HANDLE_SIZE);
        gc.setFill(emphasisColor);
        gc.fillOval(handlePosX - HANDLE_INNER_SIZE_HALF, handlePosY - HANDLE_INNER_SIZE_HALF, HANDLE_INNER_SIZE, HANDLE_INNER_SIZE);
        gc.strokeOval(handlePosX - HANDLE_INNER_SIZE_HALF, handlePosY - HANDLE_INNER_SIZE_HALF, HANDLE_INNER_SIZE, HANDLE_INNER_SIZE);

        // draw crosshairs
//        double size = 10;
//        gc.setStroke(Color.ORANGE.darker());
//        gc.strokeLine(-size, 0, size, 0);
//        gc.strokeLine(0, -size, 0, size);
//
//        gc.setStroke(Color.GREEN.darker());
//        gc.strokeLine(-size, -size, size, size);
//        gc.strokeLine(-size, size, size, -size);
//
//        gc.setStroke(Color.GREEN.darker());
//        size = 0.1;
//        gc.strokeOval(-size, -size, size * 2, size * 2);
//        size = 0.5;
//        gc.strokeOval(-size, -size, size * 2, size * 2);
//        size = 1.0;
//        gc.strokeOval(-size, -size, size * 2, size * 2);
//        size = 1.5;
//        gc.strokeOval(-size, -size, size * 2, size * 2);
//        size = 2.0;
//        gc.strokeOval(-size, -size, size * 2, size * 2);

    }

    public static Point2d calculateTriangleOuterBoundsIntersection(double a1x, double a1y, double a2x, double a2y, double b1x, double b1y, double b2x, double b2y, boolean trim) throws NotAvailableException {

        double am = (a2y - a1y) / (a2x - a1x);
        double ab = a1y - (am * a1x);

        double bm = ((b2x - b1x != 0) ? ((b2y - b1y) / (b2x - b1x)) : Double.POSITIVE_INFINITY);
        double bb = b1y - (bm * b1x);

        if (am == bm) {
            throw new NotAvailableException("No intersection found! m = " + am);
        }

        // if vector a is vertical, than we already know the value of if
        double x = bm == Double.POSITIVE_INFINITY ? b2x : (bb - ab) / (am - bm);
        double y = am * x + ab;

        // trim to triangle border
        if (trim) {
            if (a1x < a2x) {
                x = Math.max(a1x, Math.min(a2x, x));
            } else {
                x = Math.max(a2x, Math.min(a1x, x));
            }

            if (a1y < a2y) {
                y = Math.max(a1y, Math.min(a2y, y));
            } else {
                y = Math.max(a2y, Math.min(a1y, y));
            }
        }

        return new Point2d(x, y);
    }

    public double getHandlePosX() {
        return handlePosX + PADDING;
    }

    public double getHandlePosY() {
        return handlePosY + PADDING;
    }

    public double getComfort() {
        return comfortProperty.get();
    }

    public double getEconomy() {
        return economyProperty.get();
    }

    public double getSecurity() {
        return securityProperty.get();
    }

    public double getCategoryValue(final Category category) {
        switch (category) {
            case ECONOMY:
                return getEconomy();
            case COMFORT:
                return getComfort();
            case SECURITY:
                return getSecurity();
            case SAFETY:
                return 1d;
            case UNKNOWN:
            default:
                return 0d;

        }
    }

    public SimpleDoubleProperty economyProperty() {
        return economyProperty;
    }

    public SimpleDoubleProperty securityProperty() {
        return securityProperty;
    }

    public SimpleDoubleProperty comfortProperty() {
        return comfortProperty;
    }

    public Category getPrimaryEmphasisCategory() {
        return primaryEmphasisCategoryProperty.get();
    }

    public SimpleObjectProperty<Category> primaryEmphasisCategoryProperty() {
        return primaryEmphasisCategoryProperty;
    }
}
