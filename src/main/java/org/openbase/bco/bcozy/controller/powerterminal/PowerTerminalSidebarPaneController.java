package org.openbase.bco.bcozy.controller.powerterminal;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXDatePicker;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import org.openbase.bco.bcozy.controller.powerterminal.chartattributes.DateRange;
import org.openbase.bco.bcozy.controller.powerterminal.chartattributes.Granularity;
import org.openbase.bco.bcozy.controller.powerterminal.chartattributes.Unit;
import org.openbase.bco.bcozy.controller.powerterminal.chartattributes.VisualizationType;
import org.openbase.bco.bcozy.model.powerterminal.ChartStateModel;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.visual.javafx.control.AbstractFXController;

import java.time.LocalDate;
import java.time.ZoneId;

public class PowerTerminalSidebarPaneController extends AbstractFXController {

    public static final ZoneId TIME_ZONE_ID = ZoneId.of("GMT+2");
    @FXML
    private JFXComboBox<VisualizationType> selectVisualizationTypeBox;
    @FXML
    private JFXComboBox<Unit> selectUnitBox;
    @FXML
    private JFXComboBox<Granularity> selectGranularityBox;
    @FXML
    private JFXCheckBox selectDateNowCheckBox;
    @FXML
    private JFXDatePicker selectStartDatePicker;
    @FXML
    private JFXDatePicker selectEndDatePicker;
    @FXML
    private Text dateErrorMessage;

    private BooleanBinding dateValid;
    private ObjectProperty<DateRange> dateRange = new SimpleObjectProperty<>();
    private ChartStateModel chartStateModel;


    @Override
    public void updateDynamicContent() throws CouldNotPerformException {

    }

    @Override
    public void initContent() throws InitializationException {
        selectVisualizationTypeBox.getItems().addAll(VisualizationType.getSelectableTypes());
        selectVisualizationTypeBox.getSelectionModel().select(0);
        selectGranularityBox.getItems().addAll(Granularity.values());
        selectGranularityBox.getSelectionModel().select(0);
        selectUnitBox.getItems().addAll(Unit.values());
        selectUnitBox.getSelectionModel().select(0);
        selectStartDatePicker.disableProperty().bind(selectDateNowCheckBox.selectedProperty());
        selectEndDatePicker.disableProperty().bind(selectDateNowCheckBox.selectedProperty());
        selectStartDatePicker.setValue(LocalDate.now(TIME_ZONE_ID).minusDays(1));
        selectEndDatePicker.setValue(LocalDate.now(TIME_ZONE_ID));
        dateRange.set(new DateRange(selectStartDatePicker.getValue(), selectEndDatePicker.getValue()));

        chartStateModel = new ChartStateModel(selectVisualizationTypeBox.valueProperty(), selectUnitBox.valueProperty(),
                selectGranularityBox.valueProperty(), dateRange);

        selectStartDatePicker.valueProperty().addListener((dont, care, newStartDate) -> {
            DateRange dateRange = new DateRange(newStartDate, this.dateRange.get().getEndDate());
            if(dateRange.isValid())
                this.dateRange.set(dateRange);
        });
        selectEndDatePicker.valueProperty().addListener((dont, care, newEndDate) -> {
            DateRange dateRange = new DateRange(this.dateRange.get().getStartDate(), newEndDate);
            if(dateRange.isValid())
                this.dateRange.set(dateRange);
        });
        dateValid = Bindings.createBooleanBinding(this::isDateValid,
                selectStartDatePicker.valueProperty(), selectEndDatePicker.valueProperty());
        dateErrorMessage.visibleProperty().bind(dateValid.not());
    }

    private boolean isDateValid() {//TODO: Replace with function from dateRange
        return selectStartDatePicker.getValue().isBefore(selectEndDatePicker.getValue())
                && selectEndDatePicker.getValue().isBefore(LocalDate.now(TIME_ZONE_ID).plusDays(1));
    }

    public ChartStateModel getChartStateModel() {
        return chartStateModel;
    }

}
