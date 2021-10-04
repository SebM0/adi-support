package com.axway.adi.tools;

import java.util.*;
import com.axway.adi.tools.util.AlertHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableView;

import static com.axway.adi.tools.util.MemorySizeFormatter.toHumanAndRealSize;
import static java.util.stream.Collectors.*;
import static javafx.scene.control.Alert.AlertType.INFORMATION;

public class ActivityController {
    private static final Map<String, String> CHANNELS = Map.of("abs:default", "default", //
                                                               "abs:indicator_computing", "computing", //
                                                               "abs:cube_computing", "cube", //
                                                               "abs:data_integration", "integ", //
                                                               "abs:ws_data_integration", "ws");
    private ActivityMain parent;
    private final LinkedList<JsonObject> history = new LinkedList<>();
    private final Set<String> currentHandlers = new HashSet<>();

    // Control bindings
    public Slider timeSlider;
    public TableView<Map<String, Object>> pressure;
    public Label timeLabel;
    public LineChart<String, Number> timeChart;
    private XYChart.Series<String, Number> memSeries;
    public PieChart memPie;
    private PieChart.Data sliceDB;
    private PieChart.Data sliceQR;
    private PieChart.Data sliceCUBE;
    private PieChart.Data sliceUNK;
    private PieChart.Data sliceFREE;
    public BarChart<String, Number> channelStack;
    private final Map<String, XYChart.Data<String,Number>> channelData = new HashMap<>();
    public Button playButton;
    public Button clearButton;
    public Button detailsButton;
    public Label liveLabel;
    public Label corrLabel;

    void bindControls(ActivityMain activityMain) {
        this.parent = activityMain;
        pressure.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> onEventSelected(newValue));
        timeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int index = Math.min(newValue.intValue(), history.size()) - 1;
            if (index >= 0) {
                JsonObject object = history.get(index);
                updateData(object);
            } else {
                updateData(null);
            }
        });
        memSeries = new XYChart.Series<>();
        memSeries.setName("memory (MB)");
        timeChart.getData().add(memSeries);
        timeChart.getXAxis().setTickLabelsVisible(false);
        timeChart.getXAxis().setTickMarkVisible(false);
        sliceDB = new PieChart.Data("DB", 0);
        sliceQR = new PieChart.Data("QR", 0);
        sliceCUBE = new PieChart.Data("CUBE", 0);
        sliceUNK = new PieChart.Data("?", 0);
        sliceFREE = new PieChart.Data("Free", 0);
        memPie.getData().clear();
        memPie.getData().add(sliceDB);
        memPie.getData().add(sliceQR);
        memPie.getData().add(sliceCUBE);
        memPie.getData().add(sliceUNK);
        memPie.getData().add(sliceFREE);
        memPie.setLegendVisible(false);
        sliceDB.getNode().setStyle("-fx-pie-color: #ff5700;");
        sliceQR.getNode().setStyle("-fx-pie-color: #ffd700;");
        sliceCUBE.getNode().setStyle("-fx-pie-color: #4400ff;");
        sliceUNK.getNode().setStyle("-fx-pie-color: #555555;");
        sliceFREE.getNode().setStyle("-fx-pie-color: #2fff2f;");
        channelStack.getData().clear();
        XYChart.Series<String, Number> dataSeries1 = new XYChart.Series<>();
        dataSeries1.setName("Channels");
        channelStack.getData().add(dataSeries1);
        CHANNELS.forEach((channel, label) -> {
            XYChart.Data<String, Number> data = new XYChart.Data<>(label, 0);
            dataSeries1.getData().add(data);
            channelData.put(channel, data);
        });
        clearButton.setDisable(true);
    }

    synchronized void insertLine(String line) {
        try {
            JsonObject object = JsonParser.parseString(line).getAsJsonObject();
            if (object != null) {
                String timeWithMillis = object.get("time").getAsString();
                String time = timeWithMillis.substring(0, timeWithMillis.lastIndexOf(',')); // remove millis
                object.addProperty("time", time);

                String metric = object.get("metric").getAsString();

                JsonObject newArgs = object.get("args").getAsJsonObject();
                JsonObject previousObject = null;
                Iterator<JsonObject> previousObjectIterator = history.descendingIterator();
                while (previousObjectIterator.hasNext()) {
                    JsonObject previous = previousObjectIterator.next();
                    int cmp = time.compareTo(previous.get("time").getAsString());
                    if (cmp == 0) {
                        previousObject = previous;
                        break;
                    } else if (cmp > 0) {
                        break; // pass
                    }
                }
                JsonObject persistedArgs;
                int index;
                if (previousObject != null) {
                    // concat new object to previous
                    persistedArgs = previousObject.get("args").getAsJsonObject();
                    newArgs.entrySet().forEach(e -> {
                        String key = e.getKey();
                        if ("plans".equals(key) && "runtimeSummary".equals(metric)) {
                            key = "r:plans";
                        }
                        persistedArgs.add(key, e.getValue());
                    });
                    index = previousObject.get("index").getAsInt();
                } else {
                    history.add(object);
                    index = history.size();
                    object.addProperty("index", index);
                    //if (history.size() > 100) {
                    //    history.remove(history.firstKey());
                    //}
                    persistedArgs = newArgs;
                    if ("runtimeSummary".equals(metric)) {
                        persistedArgs.add("r:plans", persistedArgs.get("plans"));
                        persistedArgs.remove("plans");
                    }
                }
                if ("memorySummary".equals(metric)) {
                    long jvmUsed = Long.parseLong(newArgs.get("jvmUsed").getAsString());
                    long jvmMax = Long.parseLong(newArgs.get("jvmMax").getAsString());
                    int jvmUsedRatio = (int) Math.round(100.0 * jvmUsed / jvmMax);
                    persistedArgs.addProperty("jvmUsedRatio", jvmUsedRatio);
                    Platform.runLater(() -> {
                        try {
                            int size = history.size();
                            memSeries.getData().add(new XYChart.Data<>(Integer.toString(size), jvmUsedRatio));
                            timeSlider.setMax(size);
                            timeSlider.adjustValue(size);
                        } catch (IllegalArgumentException e) {
                            // skip
                        }
                    });
                    // Pressure detection
                    if (jvmUsedRatio > 80) {
                        increasePressure("Memory", time, index);
                    } else {
                        releasePressure("Memory", time, index);
                    }
                } else if ("runtimeSummary".equals(metric)) {
                    Set<String> handlers = currentHandlers.stream().filter(c -> c.startsWith("h:")).collect(toSet());
                    newArgs.entrySet().forEach(e -> {
                        String key = e.getKey();
                        if (key.startsWith("h:")) {
                            handlers.remove(key);
                        }
                        String value = e.getValue().getAsString();
                        String[] split = value.split("/");
                        if (split.length == 3) {
                            int waiting = Integer.parseInt(split[2]);
                            if (waiting > 1) {
                                increasePressure(key, time, index);
                            } else {
                                releasePressure(key, time, index);
                            }
                        } else {
                            int running = Integer.parseInt(split[0]);
                            if (running > 10) {
                                increasePressure(key, time, index);
                            } else {
                                releasePressure(key, time, index);
                            }
                        }
                    });
                    handlers.forEach(h -> releasePressure(h, time, index)); // not mentioned in runtime summary => no pressure
                }
            }
        } catch (Exception e) {
            //skip
        }
    }

    void increasePressure(String item, String time, int index) {
        if (currentHandlers.add(item)) {
            insertMessage(item + " under pressure", time, index);
        }
    }

    void releasePressure(String item, String time, int index) {
        if (currentHandlers.remove(item)) {
            insertMessage(item + " back to normal", time, index);
        }
    }

    void insertMessage(String msg, String time, int index) {
        //        events.add(object);
        Platform.runLater(() -> pressure.getItems().add(Map.of("time", time, "msg", msg, "index", index)));
    }

    void updateData(JsonObject object) {
        if (object == null) {
            timeLabel.setText("-");
            sliceDB.setPieValue(0);
            sliceQR.setPieValue(0);
            sliceCUBE.setPieValue(0);
            sliceUNK.setPieValue(0);
            sliceFREE.setPieValue(0);
            liveLabel.setText("0");
            corrLabel.setText("0");
            channelData.values().forEach(data -> data.setYValue(0));
            return;
        }
        timeLabel.setText(object.get("time").getAsString());
        JsonObject args = object.get("args").getAsJsonObject();
        if (args.has("jvmUsed")) {
            long DB = args.get("memtables").getAsLong() + args.get("alive").getAsLong() + args.get("hvOpen").getAsLong() + args.get("ssTableCache").getAsLong()
                    + args.get("absorptionQueues").getAsLong();
            long WF = 0;
            try {
                WF = args.get("plans").getAsLong();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            long CB = args.get("cubeCache").getAsLong();
            long TOT = args.get("jvmUsed").getAsLong();
            long FREE = args.get("jvmMax").getAsLong() - TOT;
            long UNK = TOT - CB - WF - DB;

            sliceDB.setPieValue( (double) DB / 1_000_000);
            sliceQR.setPieValue( (double) WF / 1_000_000);
            sliceCUBE.setPieValue( (double) CB / 1_000_000);
            sliceUNK.setPieValue( (double) UNK / 1_000_000);
            sliceFREE.setPieValue( (double) FREE / 1_000_000);
        }
        if (args.has("abs:default")) {
            String live = args.get("live").getAsString();
            String[] comp = live.split("/");
            liveLabel.setText(comp[2]);
            String correction = args.get("correction").getAsString();
            comp = correction.split("/");
            corrLabel.setText(comp[2]);
            channelData.forEach((channel, data) -> {
                String value = args.get(channel).getAsString();
                String[] split = value.split("/");
                int total = Integer.parseInt(split[0]) + Integer.parseInt(split[2]);
                data.setYValue(total);
            });
        }
    }

    public void onLast(ActionEvent actionEvent) {
        timeSlider.adjustValue(timeSlider.getMax());
        actionEvent.consume();
    }

    public void onPrevious(ActionEvent actionEvent) {
        int value = (int) timeSlider.getValue();
        if (value > 0) {
            timeSlider.adjustValue(value - 1);
        }
        actionEvent.consume();
    }

    public void onNext(ActionEvent actionEvent) {
        int value = (int) timeSlider.getValue();
        if (value < timeSlider.getMax()) {
            timeSlider.adjustValue(value + 1);
        }
        actionEvent.consume();
    }

    public void onPlay(ActionEvent actionEvent) {
        boolean playing = !parent.isPlaying();
        parent.setPlaying(playing);
        playButton.setText(playing ? "Pause" :"Play");
        clearButton.setDisable(playing);
        actionEvent.consume();
    }

    public void onClear(ActionEvent actionEvent) {
        memSeries.getData().clear();
        pressure.getItems().clear();
        timeSlider.setMax(0);
        currentHandlers.clear();
        history.clear();
        timeSlider.adjustValue(0);
        actionEvent.consume();
    }

    public void onDetails(ActionEvent actionEvent) {
        int index = (int)timeSlider.getValue() - 1;
        JsonObject object = index >= 0 ? history.get(index) : null;
        if (object != null) {
            StringBuilder sb = new StringBuilder();
            String time = object.get("time").getAsString();
            JsonObject memArgs = object.get("args").getAsJsonObject();
            if (!memArgs.has("jvmUsed")) {
                memArgs = null;
                // lookup in previous
                int lookup = index;
                while (--lookup > 0) {
                    JsonObject prevObject = history.get(index);
                    if (prevObject.get("args").getAsJsonObject().has("jvmUsed")) {
                        memArgs = prevObject.get("args").getAsJsonObject();
                        time = prevObject.get("time").getAsString();
                    }
                }
            }
            if (memArgs != null) {
                long memtables = memArgs.get("memtables").getAsLong();
                long alive = memArgs.get("alive").getAsLong();
                long hvOpen = memArgs.get("hvOpen").getAsLong();
                long ssTableCache = memArgs.get("ssTableCache").getAsLong();
                long absorptionQueues = memArgs.get("absorptionQueues").getAsLong();
                long DB = memtables + alive + hvOpen + ssTableCache + absorptionQueues;
                long plans = memArgs.get("plans").getAsLong();
                long cubeCache = memArgs.get("cubeCache").getAsLong();
                long jvmUsed = memArgs.get("jvmUsed").getAsLong();
                long jvmMax = memArgs.get("jvmMax").getAsLong();
                sb.append("Memory: ").append(time).append("\n");
                sb.append("Max: ").append(toHumanAndRealSize(jvmMax)).append("\n");
                sb.append("Used: ").append(toHumanAndRealSize(jvmUsed)).append("\n");
                sb.append("- DB: ").append(toHumanAndRealSize(DB)).append("\n");
                sb.append("    - memtables: ").append(toHumanAndRealSize(memtables)).append("\n");
                sb.append("    - alive: ").append(toHumanAndRealSize(alive)).append("\n");
                sb.append("    - hvOpen: ").append(toHumanAndRealSize(hvOpen)).append("\n");
                sb.append("    - ssTableCache: ").append(toHumanAndRealSize(ssTableCache)).append("\n");
                sb.append("    - absorptionQueues: ").append(toHumanAndRealSize(absorptionQueues)).append("\n");
                sb.append("- QR plans: ").append(toHumanAndRealSize(plans)).append("\n");
                sb.append("- Cubes: ").append(toHumanAndRealSize(cubeCache)).append("\n");
            }
            time = object.get("time").getAsString();
            JsonObject rtArgs = object.get("args").getAsJsonObject();
            if (!rtArgs.has("abs:default")) {
                rtArgs = null;
                // lookup in previous
                int lookup = index;
                while (--lookup > 0) {
                    JsonObject prevObject = history.get(index);
                    if (prevObject.get("args").getAsJsonObject().has("abs:default")) {
                        rtArgs = prevObject.get("args").getAsJsonObject();
                        time = prevObject.get("time").getAsString();
                    }
                }
            }
            if (rtArgs != null) {
                sb.append("\nRuntime: ").append(time);
                sb.append("\nAbsorption channels\n");
                for (Map.Entry<String, String> entry : CHANNELS.entrySet()) {
                    String channel = entry.getKey();
                    String label = entry.getValue();
                    String value = rtArgs.get(channel).getAsString();
                    String[] split = value.split("/");
                    sb.append("- ").append(label).append(": used=").append(split[0]).append(", max=").append(split[1]).append(", wait=").append(split[2]).append("\n");
                }
                {
                    String live = rtArgs.get("live").getAsString();
                    String[] split = live.split("/");
                    sb.append("Computings:\n- live:      run=").append(split[0]).append(", max=").append(split[1]).append(", wait=").append(split[2]).append("\n");
                    String correction = rtArgs.get("correction").getAsString();
                    split = correction.split("/");
                    sb.append("- correction: run=").append(split[0]).append(", max=").append(split[1]).append(", wait=").append(split[2]).append("\n");
                }
                List<String> handlers = rtArgs.keySet().stream().filter(k -> k.startsWith("h:")).collect(toList());
                if (!handlers.isEmpty()) {
                    sb.append("Pending messages by interceptor factory");
                    for (String h : handlers) {
                        sb.append("\n- ").append(h.substring(2)).append(": ").append(rtArgs.get(h).getAsString());
                    }
                }
            }
            AlertHelper.show(INFORMATION, sb.toString());
        }
        actionEvent.consume();
    }

    public void onEventSelected(Map<String, Object> selectedItem) {
        if (selectedItem != null) {
            int index = (Integer) selectedItem.get("index");
            timeSlider.adjustValue(index);
        }
    }
}
