package org.openbase.bco.bcozy.controller.powerterminal;

import com.google.protobuf.Message;
import eu.hansolo.fx.charts.heatmap.HeatMap;
import javafx.animation.Interpolator;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;
import javafx.stage.Screen;
import org.openbase.bco.bcozy.controller.powerterminal.heatmapattributes.HeatmapValues;
import org.openbase.bco.bcozy.controller.powerterminal.heatmapattributes.SpotsPosition;
import org.openbase.bco.bcozy.view.Constants;
import org.openbase.bco.bcozy.view.location.DynamicUnitPolygon;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.unit.PowerConsumptionSensor;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.Observer;
import org.openbase.rct.Transform;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitTemplateType;
import org.openbase.type.domotic.unit.location.LocationConfigType;
import org.openbase.type.geometry.AxisAlignedBoundingBox3DFloatType;
import org.openbase.type.geometry.TranslationType;
import org.openbase.type.math.Vec3DDoubleType;
import org.openbase.type.spatial.ShapeType;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3d;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Heatmap extends Pane {

    private CustomUnitPool unitPool;

    public final int radiusSpots = 100;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(eu.hansolo.fx.charts.heatmap.HeatMap.class);


    public Heatmap() {
        try {
            unitPool = new CustomUnitPool();

            unitPool.init((Filter<UnitConfigType.UnitConfig>) unitConfig -> {
                return unitConfig.getUnitType() != UnitTemplateType.UnitTemplate.UnitType.POWER_CONSUMPTION_SENSOR;
            });

            unitPool.activate();

            UnitConfigType.UnitConfig rootLocationConfig = Registries.getUnitRegistry().getRootLocationConfig();

            HeatmapValues heatmapValues = initHeatmap(rootLocationConfig);

            //TODO: thousands warnings, heatmap crashed
            /*unitPool.addObserver(new Observer<ServiceStateProvider<Message>, Message>() {
                @Override
                public void update(ServiceStateProvider<Message> source, Message data) throws Exception {
                    updateHeatmap(heatmapValues);
                }
            }); */

            updateHeatmap(heatmapValues);

        } catch (CouldNotPerformException  ex) {
            ExceptionPrinter.printHistory("Could not instantiate CustomUnitPool", ex, logger);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            ExceptionPrinter.printHistory("Could not instantiate CustomUnitPool", ex, logger);
        }
    }

    private HeatmapValues initHeatmap(UnitConfigType.UnitConfig rootLocationConfig) {
        List<List<Point2D>> rooms = makeRooms();
        double xTranslation = 0;
        double yTranslation = 0;

        AxisAlignedBoundingBox3DFloatType.AxisAlignedBoundingBox3DFloat rootBoundingBox = rootLocationConfig.getPlacementConfig().getShape().getBoundingBox();

        double[][] u = new double[(int)(rootBoundingBox.getDepth()*Constants.METER_TO_PIXEL + 1)][(int)(rootBoundingBox.getWidth()*Constants.METER_TO_PIXEL + 1)];
        List<SpotsPosition> spots = new ArrayList<>();

        try {
            List<Point2D> point2DS = DynamicUnitPolygon.loadShapeVertices(rootLocationConfig);
            xTranslation = Math.abs(point2DS.get(0).getX());
            yTranslation = Math.abs(point2DS.get(0).getY());
            this.setTranslateY(-xTranslation);
            this.setTranslateX(-yTranslation);
          /*  for (Point2D point2D : point2DS) {
                System.out.println("X wert :" + point2D.getX());
                u[(int) (point2D.getY()+xTranslation)][(int) (point2D.getX()+yTranslation)] = 1;
                spots.add(new SpotsPosition((int)(point2D.getY()+xTranslation), (int)(point2D.getX()+yTranslation), 1));
            } */
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (CouldNotPerformException e) {
            e.printStackTrace();
        }


       int unitListPosition = -1;
       for (UnitRemote<? extends Message> unit : unitPool.getInternalUnitList()) {
           unitListPosition++;
           try {
               unit.waitForData(Constants.TRANSFORMATION_TIMEOUT, TimeUnit.MILLISECONDS);
               Future<Transform> transform = Registries.getUnitRegistry().getUnitTransformationFuture(unit.getConfig(), rootLocationConfig);
               TranslationType.Translation unitPosition = unit.getUnitPosition();
               Point3d unitPoint = new Point3d(unitPosition.getX(), unitPosition.getY(), unitPosition.getZ());

               //Wait for transformation of unitPoint but use getUnitPositionGlobalPoint3D because Position of unitPoint is wrong
               transform.get(Constants.TRANSFORMATION_TIMEOUT, TimeUnit.MILLISECONDS).getTransform().transform(unitPoint);

               int unitPointGlobalX = (int) (unit.getUnitPositionGlobalPoint3d().x*Constants.METER_TO_PIXEL + xTranslation);
               int unitPointGlobalY = (int) (unit.getUnitPositionGlobalPoint3d().y*Constants.METER_TO_PIXEL + yTranslation);
               if (unitPointGlobalX >= 0 && unitPointGlobalX < u[0].length
                       && unitPointGlobalY >= 0 && unitPointGlobalY < u.length) {
                   spots.add(new SpotsPosition(unitPointGlobalY, unitPointGlobalX, 0, unitListPosition));
               }
           } catch (CouldNotPerformException ex) {
               //ExceptionPrinter.printHistory("Could not get location units", ex, logger);
           } catch (InterruptedException ex) {
               Thread.currentThread().interrupt();
              // ExceptionPrinter.printHistory("Could not get location units", ex, logger);;
           } catch (ExecutionException ex) {
              // ExceptionPrinter.printHistory("Could not get location units", ex, logger);
           } catch (TimeoutException ex) {
              // ExceptionPrinter.printHistory("Could not get location units", ex, logger);
           }
       }

        return new HeatmapValues(rooms, spots, u);
    }

    private List<List<Point2D>> makeRooms() {
        List<UnitConfigType.UnitConfig> roomConfigs = null;
        List<List<Point2D>> rooms = new ArrayList<>();
        try {
            roomConfigs = Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitTemplateType.UnitTemplate.UnitType.LOCATION);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not get location units", ex, logger);
        }
        for (UnitConfigType.UnitConfig roomConfig : roomConfigs) {
            LocationConfigType.LocationConfig.LocationType locationType = roomConfig.getLocationConfig().getLocationType();
            if (!locationType.equals(LocationConfigType.LocationConfig.LocationType.TILE)) {
                continue;
            }

            try {
                List<Point2D> roomPoints = DynamicUnitPolygon.loadShapeVertices(roomConfig);
                rooms.add(roomPoints);
               /* System.out.println(LabelProcessor.getBestMatch(roomConfig.getLabel(), "?"));
                for (Point2D roomPoint : roomPoints) {
                    System.out.println("X wert: " + roomPoint.getX() + " Y " + roomPoint.getY());
                } */

            } catch (InterruptedException ex) {
                ExceptionPrinter.printHistory("Could not get location units", ex, logger);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not get location units", ex, logger);
            }
        }
        return rooms;
    }

     private void updateHeatmap(HeatmapValues heatmapValues) {
        int runnings = 3;
        List<SpotsPosition> spots = heatmapValues.getSpots();
        double[][] u = heatmapValues.getU();

         for (SpotsPosition spot : spots) {
             PowerConsumptionSensor powerConsumptionUnit = (PowerConsumptionSensor) unitPool.getInternalUnitList().get(spot.unitListPosition);
             try {
                 double current = powerConsumptionUnit.getPowerConsumptionState().getCurrent() / 16;
                 current = 1;
                 u[spot.spotsPositionx][spot.spotsPositiony] = current;
                 spot.value = current;
             } catch (NotAvailableException e) {
                 e.printStackTrace();
             }
        }
        heatmapValues.setSpots(spots);
        heatmapValues.setU(u);
        this.getChildren().clear();
        this.getChildren().add(generateHeatmapWithLibrary(heatmapValues, runnings));
     }


    private HeatMap generateHeatmapWithLibrary(HeatmapValues heatmapValues, int runnings) {
        calculateHeatMap(heatmapValues, runnings);

        HeatMap heatmap = new eu.hansolo.fx.charts.heatmap.HeatMap(heatmapValues.getU().length, heatmapValues.getU()[0].length);
        heatmap.setOpacity(0.8);

        for (SpotsPosition spot : heatmapValues.getSpots()) {
            heatmap.addSpot(spot.spotsPositionx, spot.spotsPositiony, createEventImage(heatmapValues, spot, runnings), radiusSpots*runnings, radiusSpots*runnings);
        }
        return heatmap;
    }

    public Image createEventImage(HeatmapValues heatmapValues, SpotsPosition spot, int runnings) {
        Double radius = (double) runnings*radiusSpots;
        double[][] u = heatmapValues.getU();

        Stop[] stops = new Stop[runnings+1];
        for (int i = 0; i < runnings + 1; i++) {
            double[] opacity = new double[4];
            if (i != runnings) {
                if (spot.spotsPositionx+i < u.length)
                    opacity[0] = u[spot.spotsPositionx+i][spot.spotsPositiony];
                else
                    opacity[0] = 1;

                if (spot.spotsPositiony+i < u[spot.spotsPositiony].length)
                    opacity[1] = u[spot.spotsPositionx][spot.spotsPositiony+i];
                else
                    opacity[1] = 1;

                if (spot.spotsPositionx-i > 0)
                    opacity[2] = u[spot.spotsPositionx-i][spot.spotsPositiony];
                else
                    opacity[2] = 1;

                if (spot.spotsPositiony-i > 0)
                    opacity[3] = u[spot.spotsPositionx][spot.spotsPositiony-i];
                else
                    opacity[3] = 1;
            }

            Arrays.sort(opacity);
            stops[i] = new Stop(i * 0.1, Color.rgb(255, 255, 255, opacity[0]));
        }

        int size = (int) (radius * 2);
        WritableImage raster        = new WritableImage(size, size);
        PixelWriter pixelWriter   = raster.getPixelWriter();
        double        maxDistFactor = 1 / radius;
        Color pixelColor;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double deltaX   = radius - x;
                double deltaY   = radius - y;
                double distance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
                double fraction = maxDistFactor * distance;
                for (int i = 0; i < stops.length-1; i++) {
                    if (Double.compare(fraction, stops[i].getOffset()) >= 0 && Double.compare(fraction, stops[i + 1].getOffset()) <= 0) {
                        int xGlobal = (int) (spot.spotsPositionx + (size/2 - x));
                        int yGlobal = (int) (spot.spotsPositiony + (size/2 - y));
                        if (heatmapValues.isInsideRoom(xGlobal, yGlobal)) {
                            pixelColor = (Color) Interpolator.LINEAR.interpolate(stops[i].getColor(), stops[i + 1].getColor(), (fraction - stops[i].getOffset()) / 0.1);

                            //pixelColor = new Color(0,0,0,1);
                        }
                        else
                        {
                            pixelColor = (Color) Interpolator.LINEAR.interpolate(stops[i].getColor(), stops[i + 1].getColor(), (fraction - stops[i].getOffset()) / 0.1);
                        }
                        pixelWriter.setColor(x, y, pixelColor);
                        break;
                    }
                }
            }
        }
        return raster;
    }

    private void calculateHeatMap (HeatmapValues heatmapValues, int runnings) {
        double[][] u = heatmapValues.getU();
        List<SpotsPosition> spots= heatmapValues.getSpots();
        double h = 1;
        double delta_t = 0.1;
        double[][] v = new double[u.length][u[0].length];
        for (int runs = 0; runs < runnings; runs ++) {

            for (int col = 1; col < u.length - 1; col++) {
                for (int row = 1; row < u[col].length - 1; row++) {
                    v[col][row] = (u[col - 1][row] + u[col + 1][row] + u[col][row - 1] + u[col][row + 1]
                            - 4*u[col][row]) / (h * h);
                }
            }

            for (int col = 0; col < u.length; col++) {
                for (int row = 0; row < u[col].length; row++) {
                    u[col][row] = u[col][row] + delta_t * v[col][row];
                }
            }

            for (SpotsPosition spot : spots) {
                u[spot.spotsPositionx][spot.spotsPositiony] = spot.value;
            }
        }
        heatmapValues.setU(u);
    }
}
