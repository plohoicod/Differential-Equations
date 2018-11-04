package sample;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Controller {
    @FXML
    private Button calculateButton;
    @FXML
    private TextField x0Field, y0Field, xMaxValueField, gridStepsNumberField, minGridStepsNumberField;
    @FXML
    private LineChart lineChart, errorChart, errorForDifferentGridStepsChart;
    @FXML
    private CheckBox showExactSolutionChartCheckBox, showEulersMethodCheckBox, showImprovedEulersMethodCheckBox, showRungeKuttaMethodCheckBox;

    private double x0, y0, X, C;
    private long gridStepNumber, minGridStepNumber;

    private double f(double x, double y) {
        return 1/2 * Math.sin(2 * x) - y * Math.cos(x);
    }

    private double y(double x) {
        return C * Math.pow(Math.E, -Math.sin(x)) + Math.sin(x) - 1;
    }

    private void addSeriesToChart(XYChart.Series series, LineChart chart) {
        chart.getData().addAll(series);
    }

    private void addNewPointToSeries(double x, double y, XYChart.Series series) {
        series.getData().add(new XYChart.Data(String.format("%.2f", x), y));
    }

    private void addNewPointToList(double x, double y, List points) {
        points.add(new Pair<>(x, y));
    }

    private XYChart.Series createSeriesFromList(List<Pair<Double, Double>> points, String name) {
        XYChart.Series series = new XYChart.Series();
        series.setName(name);
        for (Pair<Double, Double> p : points) {
            addNewPointToSeries(p.getKey(), p.getValue(), series);
        }
        return series;
    }

    private List getErrorPoints(List<Pair<Double, Double>> pointsOfCurrentMethod) {
        List<Pair<Double, Double>> errorPoints = new ArrayList<>();
        for (int i = 0; i < pointsOfCurrentMethod.size(); i++) {
            double x = pointsOfCurrentMethod.get(i).getKey();
            double error = Math.abs(y(x) - pointsOfCurrentMethod.get(i).getValue());
            addNewPointToList(x, error, errorPoints);
        }
        return errorPoints;
    }

    private List getSeriesByExactSolution(double x0, double y0, double X, long gridStepNumber) {
        List<Pair<Double, Double>> points = new ArrayList<>();
        addNewPointToList(x0, y0, points);
        double h = (X - x0) / gridStepNumber;
        double x = x0, y;
        for (int i = 0; i < gridStepNumber; i++) {
            x = x + h;
            y = y(x);
            addNewPointToList(x, y, points);
        }
        return points;
    }



    private List getSeriesByEulersMethod(double x0, double y0, double X, long gridStepNumber) {
        List<Pair<Double, Double>> points = new ArrayList<>();
        addNewPointToList(x0, y0, points);
        double h = (X - x0) / gridStepNumber;
        double x = x0, y = y0;
        for (int i = 0; i < gridStepNumber; i++) {
            y = y + h * f(x, y);
            x = x + h;
            addNewPointToList(x, y, points);
        }
        return points;
    }

    private List getSeriesByImprovedEulersMethod(double x0, double y0, double X, long gridStepNumber) {
        List<Pair<Double, Double>> points = new ArrayList<>();
        addNewPointToList(x0, y0, points);
        double h = (X - x0) / gridStepNumber;
        double x = x0, y = y0;
        for (int i = 0; i < gridStepNumber; i++) {
            y = y + h * f(x + h / 2, y + h / 2 * f(x, y));
            x = x + h;
            addNewPointToList(x, y, points);
        }
        return points;
    }

    private List getSeriesByRungeKuttaMethod(double x0, double y0, double X, long gridStepNumber) {
        List<Pair<Double, Double>> points = new ArrayList<>();
        addNewPointToList(x0, y0, points);
        double h = (X - x0) / gridStepNumber;
        double x = x0, y = y0, k1, k2, k3, k4;
        for (int i = 0; i < gridStepNumber; i++) {
            k1 = f(x, y);
            k2 = f(x + h / 2, y + h * k1 / 2);
            k3 = f(x + h / 2, y + h * k2 / 2);
            k4 = f(x + h, y + h * k3);
            y = y + h / 6 * (k1 + 2 * k2 + 2 * k3 + k4);
            x = x + h;
            addNewPointToList(x, y, points);
        }
        return points;
    }

    @FXML
    public void initialize(){
        calculateButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                lineChart.getData().clear();
                errorChart.getData().clear();
                errorForDifferentGridStepsChart.getData().clear();

                try {
                    x0 = Double.parseDouble(x0Field.getText());
                    y0 = Double.parseDouble(y0Field.getText());
                    X = Double.parseDouble(xMaxValueField.getText());
                    gridStepNumber = Long.parseLong(gridStepsNumberField.getText());
                    minGridStepNumber = Long.parseLong(minGridStepsNumberField.getText());
                    C = (y0 - Math.sin(x0) + 1) * Math.pow(Math.E, -Math.sin(x0));
                }
                catch (NumberFormatException e) {
                    System.out.println("Some field contains invalid value");
                    return;
                }

                List<Pair<Double, Double>> exactSolutionPoints = getSeriesByExactSolution(x0, y0, X, gridStepNumber);

                if (showExactSolutionChartCheckBox.isSelected()) {
                    XYChart.Series series = createSeriesFromList(exactSolutionPoints, "Exact solution");
                    addSeriesToChart(series, lineChart);
                }
                if (showEulersMethodCheckBox.isSelected()) {
                    List<Pair<Double, Double>> points = getSeriesByEulersMethod(x0, y0, X, gridStepNumber);
                    addSeriesToChart(createSeriesFromList(points, "Euler's Method"), lineChart);

                    List errorPoints = getErrorPoints(points);
                    addSeriesToChart(createSeriesFromList(errorPoints, "Euler's Method"), errorChart);

                    XYChart.Series errorSeries = new XYChart.Series();
                    for (long curStepsNumber = minGridStepNumber; curStepsNumber <= gridStepNumber; curStepsNumber++) {
                        List<Pair<Double, Double>> tmpPoints = getSeriesByEulersMethod(x0, y0, X, curStepsNumber);
                        List<Pair<Double, Double>> tmpErrorPoint = getErrorPoints(tmpPoints);
                        double mx = tmpErrorPoint.get((int)curStepsNumber).getValue();
                        for (int i = 0; i < tmpErrorPoint.size(); i++) {
                            mx = Math.max(mx, tmpErrorPoint.get(i).getValue());
                        }
                        addNewPointToSeries(curStepsNumber, mx, errorSeries);
                    }
                    errorSeries.setName("Euler's Method");
                    errorForDifferentGridStepsChart.getData().add(errorSeries);
                }
                if (showImprovedEulersMethodCheckBox.isSelected()) {
                    List points = getSeriesByImprovedEulersMethod(x0, y0, X, gridStepNumber);
                    addSeriesToChart(createSeriesFromList(points, "Improved Euler's Method"), lineChart);

                    List errorPoints = getErrorPoints(points);
                    addSeriesToChart(createSeriesFromList(errorPoints, "Improved Euler's Method"), errorChart);

                    XYChart.Series errorSeries = new XYChart.Series();
                    for (long curStepsNumber = minGridStepNumber; curStepsNumber <= gridStepNumber; curStepsNumber++) {
                        List<Pair<Double, Double>> tmpPoints = getSeriesByImprovedEulersMethod(x0, y0, X, curStepsNumber);
                        List<Pair<Double, Double>> tmpErrorPoint = getErrorPoints(tmpPoints);
                        double mx = tmpErrorPoint.get((int)curStepsNumber).getValue();
                        for (int i = 0; i < tmpErrorPoint.size(); i++) {
                            mx = Math.max(mx, tmpErrorPoint.get(i).getValue());
                        }
                        addNewPointToSeries(curStepsNumber, mx, errorSeries);
                    }
                    errorSeries.setName("Improved Euler's Method");
                    errorForDifferentGridStepsChart.getData().add(errorSeries);
                }
                if (showRungeKuttaMethodCheckBox.isSelected()) {
                    List points = getSeriesByRungeKuttaMethod(x0, y0, X, gridStepNumber);
                    addSeriesToChart(createSeriesFromList(points, "Runge-Kutta Method"), lineChart);

                    List errorPoints = getErrorPoints(points);
                    addSeriesToChart(createSeriesFromList(errorPoints, "Runge-Kutta Method"), errorChart);

                    XYChart.Series errorSeries = new XYChart.Series();
                    for (long curStepsNumber = minGridStepNumber; curStepsNumber <= gridStepNumber; curStepsNumber++) {
                        List<Pair<Double, Double>> tmpPoints = getSeriesByRungeKuttaMethod(x0, y0, X, curStepsNumber);
                        List<Pair<Double, Double>> tmpErrorPoint = getErrorPoints(tmpPoints);
                        double mx = tmpErrorPoint.get((int)curStepsNumber).getValue();
                        for (int i = 0; i < tmpErrorPoint.size(); i++) {
                            mx = Math.max(mx, tmpErrorPoint.get(i).getValue());
                        }
                        addNewPointToSeries(curStepsNumber, mx, errorSeries);
                    }
                    errorSeries.setName("Runge-Kutta Method");
                    errorForDifferentGridStepsChart.getData().add(errorSeries);
                }
            }
        });
    }

}
