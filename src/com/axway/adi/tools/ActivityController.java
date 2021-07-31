package com.axway.adi.tools;

import java.util.*;
import java.util.concurrent.atomic.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

public class ActivityController {
    private final Stage parentStage;
    private final LinkedList<JsonObject> history = new LinkedList<>();
    private final LinkedList<JsonObject> events = new LinkedList<>();

    // Control bindings
    public Slider timeSlider;
    public TreeView<String> pressure;
    public Button runButton;
    public PieChart memPie;
    public Label timeLabel;
    public LineChart<String, Number> timeChart;
    public BarChart<String, Number> channelStack;
    private XYChart.Series<String, Number> memSeries;

    public ActivityController(Stage parentStage) {
        this.parentStage = parentStage;
    }

    void bindControls() {
        timeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int index = Math.min(newValue.intValue(), history.size()) - 1;
            if (index >= 0) {
                JsonObject object = history.get(index);
                updateData(object);
            } else {
                timeLabel.setText("-");
            }
        });
        memSeries = new XYChart.Series<>();
        memSeries.setName("memory (MB)");
        timeChart.getData().add(memSeries);
        TreeItem<String> root = new TreeItem<>("Parameter events");
        root.setExpanded(true);
        pressure.setRoot(root);
    }

    void insertLine(String line, boolean init) {
        try {
            JsonObject object = JsonParser.parseString(line).getAsJsonObject();
            if (object != null) {
                if ("memorySummary".equals(object.get("metric").getAsString())) {
                    history.add(object);
                    if (history.size() > 100) {
                        history.removeFirst();
                    }
                    if (!init) {
                        Number jvmUsedRatio = Double.parseDouble(object.get("args").getAsJsonObject().get("jvmUsedRatio").getAsString().replace(',', '.'));
                        memSeries.getData().add(new XYChart.Data<>(Integer.toString(history.size()), jvmUsedRatio));
                        Platform.runLater(() -> timeSlider.adjustValue(history.size()));
                    }
                } else {
                    events.add(object);
                    if (!init) {
                        Platform.runLater(() -> {
                            String msg = object.get("args").getAsJsonObject().get("msg").getAsString();
                            pressure.getRoot().getChildren().add(new TreeItem<>(msg));
                        });
                    }
                }
            }
        } catch (Exception e) {
            //skip
        }
    }

    public void updateView() {
        Platform.runLater(() -> {
            Iterator<JsonObject> iterator = history.iterator();
            LongAdder index = new LongAdder();
            while (iterator.hasNext()) {
                JsonObject object = iterator.next();
                index.increment();
                Number jvmUsedRatio = Double.parseDouble(object.get("args").getAsJsonObject().get("jvmUsedRatio").getAsString().replace(',', '.'));
                memSeries.getData().add(new XYChart.Data<>(index.toString(), jvmUsedRatio));
            }
            timeSlider.adjustValue(history.size());
            events.forEach(object -> {
                String msg = object.get("args").getAsJsonObject().get("msg").getAsString();
                msg = msg.replace("\r\n", "\n");
                if (msg.endsWith("\n")) {
                    msg = msg.substring(0, msg.length() - 1);
                }
                pressure.getRoot().getChildren().add(new TreeItem<>(msg));
            });
        });
    }

    void updateData(JsonObject object) {
        timeLabel.setText(object.get("time").getAsString());
        JsonObject args = object.get("args").getAsJsonObject();
        long DB = args.get("memtables").getAsLong() + args.get("alive").getAsLong() + args.get("hvOpen").getAsLong() + args.get("ssTableCache").getAsLong() + args.get("absorptionQueues").getAsLong();
        long WF = args.get("plans").getAsLong();
        long CB = args.get("cubeCache").getAsLong();
        long TOT = args.get("jvmUsed").getAsLong();
        long UNK = TOT - CB - WF - DB;

        PieChart.Data slice1 = new PieChart.Data("DB", (double) DB / 1_000_000);
        PieChart.Data slice2 = new PieChart.Data("QR", (double) WF / 1_000_000);
        PieChart.Data slice3 = new PieChart.Data("CUBE", (double) CB / 1_000_000);
        PieChart.Data slice4 = new PieChart.Data("?", (double) UNK / 1_000_000);
        memPie.getData().clear();
        memPie.getData().add(slice1);
        memPie.getData().add(slice2);
        memPie.getData().add(slice3);
        memPie.getData().add(slice4);

        channelStack.getData().clear();
        XYChart.Series<String, Number> dataSeries1 = new XYChart.Series<>();
        dataSeries1.setName("Channels");
        channelStack.getData().add(dataSeries1);
        String channels = args.get("absorptionChannels").getAsString();
        for (String channel : channels.split(", ")) {
            String[] split = channel.split(": ");
            String[] split1 = split[1].split("/");
            int userPermits = Integer.parseInt(split1[1]) - Integer.parseInt(split1[0]);
            String label = split[0];
            switch (split[0]) {
                case "indicator_computing":
                    label = "computing";
                    break;
                case "cube_computing":
                    label = "cube";
                    break;
                case "data_integration":
                    label = "integ";
                    break;
                case "ws_data_integration":
                    label = "ws";
                    break;
            }
            dataSeries1.getData().add(new XYChart.Data<>(label, userPermits));
        }
    }

    public void onRun(ActionEvent actionEvent) {
        // TODO implement method
        timeSlider.adjustValue(timeSlider.getMax());
    }

}
