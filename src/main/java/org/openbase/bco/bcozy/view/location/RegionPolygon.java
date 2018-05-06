/**
 * ==================================================================
 *
 * This file is part of org.openbase.bco.bcozy.
 *
 * org.openbase.bco.bcozy is free software: you can redistribute it and modify
 * it under the terms of the GNU General Public License (Version 3)
 * as published by the Free Software Foundation.
 *
 * org.openbase.bco.bcozy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with org.openbase.bco.bcozy. If not, see <http://www.gnu.org/licenses/>.
 * ==================================================================
 */
package org.openbase.bco.bcozy.view.location;

import javafx.scene.paint.Color;
import org.openbase.bco.bcozy.view.Constants;
import org.openbase.bco.bcozy.view.InfoPane;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import rst.domotic.unit.location.LocationDataType.LocationData;

/**
 *
 */
public class RegionPolygon extends LocationPolygon {

    private boolean selectable;
    private final LocationPane locationPane;

    /**
     * The Constructor for a RegionPolygon.
     *
     * @param points The vertices of the location
     */
    public RegionPolygon(final LocationPane locationPane, final double... points) throws InstantiationException {
        super(points);
        this.locationPane = locationPane;
        this.selectable = false;
        setOnMouseClicked(event -> {
            try {
                event.consume();
                if (event.isStillSincePress()) {
                    if (event.getClickCount() == 1) {
                        locationPane.setSelectedLocation(this);
                    } else if (event.getClickCount() == 2) {
                        if (locationPane.getLastClickTarget().equals(this)) {
                            locationPane.autoFocusPolygonAnimated(this);
                        } else {
                            locationPane.getLastClickTarget().fireEvent(event.copyFor(null, locationPane.getLastClickTarget()));
                        }
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle mouse event!", ex, LOGGER);
            }
        });
        setOnMouseEntered(event -> {
            try {
                event.consume();
                mouseEntered();
                InfoPane.info(getLabel());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle mouse event!", ex, LOGGER);
            }
        });
        setOnMouseExited(event -> {
            event.consume();
            mouseLeft();
            InfoPane.info("");
        });
    }

    @Override
    public void applyDataUpdate(LocationData unitData) {
    }

    @Override
    protected void setLocationStyle() {
        this.setMainColor(Constants.REGION_FILL);
        this.setStroke(Color.WHITE);
        this.setStrokeWidth(0);
        this.setMouseTransparent(true);
    }

    @Override
    protected void changeStyleOnSelection(final boolean selected) {
        if (selected) {
            this.setMainColor(Constants.TILE_SELECTION);
        } else {
            this.setMainColor(Constants.REGION_FILL);
        }
    }

    /**
     * This method should be called to change the selectable status.
     *
     * @param selectable Whether the Region should be selectable or not.
     */
    public void changeStyleOnSelectable(final boolean selectable) {
        if (selectable) {
            this.selectable = true;
            this.getStrokeDashArray().addAll(Constants.REGION_DASH_WIDTH, Constants.REGION_DASH_WIDTH);
            this.setStrokeWidth(Constants.REGION_STROKE_WIDTH);
            this.setMouseTransparent(false);
        } else {
            this.selectable = false;
            this.getStrokeDashArray().clear();
            this.setStrokeWidth(0.0);
            this.setMouseTransparent(true);
        }
    }

    /**
     * Will be called when either the main or the custom color changes.
     * The initial values for both colors are Color.TRANSPARENT.
     *
     * @param mainColor The main color
     * @param customColor The custom color
     */
    @Override
    protected void onColorChange(final Color mainColor, final Color customColor) {
        if (customColor.equals(Color.TRANSPARENT)) {
            this.setFill(mainColor);
        } else {
            this.setFill(mainColor.interpolate(customColor, CUSTOM_COLOR_WEIGHT));
        }
    }

    /**
     * Getter for the selectable status.
     *
     * @return The selectable status.
     */
    public boolean isSelectable() {
        return selectable;
    }

    /**
     * This method should be called when the mouse enters the polygon.
     */
    private void mouseEntered() {
        this.setStrokeWidth(Constants.REGION_STROKE_WIDTH_MOUSE_OVER);
    }

    /**
     * This method should be called when the mouse leaves the polygon.
     */
    private void mouseLeft() {
        this.setStrokeWidth(Constants.REGION_STROKE_WIDTH);
    }
}
