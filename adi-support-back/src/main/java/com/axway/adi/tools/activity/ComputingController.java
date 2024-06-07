package com.axway.adi.tools.activity;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;
import javax.management.*;
import javax.management.openmbean.*;
import javax.management.remote.*;
import org.kordamp.ikonli.javafx.FontIcon;
import com.axway.adi.tools.util.TimeUtils;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import static java.lang.Thread.sleep;

public class ComputingController {
    private static final List<String> COMPUTES = List.of("Live", "Recompute");

    private AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean playing = new AtomicBoolean(true);

    // Control bindings
    public TableView<Map<String, Object>> absorbed;
    public TableView<Map<String, Object>> computed;
    public Label timeLabel;
    public LineChart<String, Number> timeChart;
    private XYChart.Series<String, Number> memSeries;
    public PieChart memPie;
    private PieChart.Data sliceLive;
    private PieChart.Data sliceRecompute;
    public BarChart<String, Number> channelStack;
    private final Map<String, XYChart.Data<String,Number>> channelData = new HashMap<>();
    public Button playButton;
    public Button clearButton;
    public Label computingLabel;
    public Label computingValue;
    private FontIcon playIcon;
    private FontIcon pauseIcon;
    public Label lateLabel;
    // JMX
    JMXConnector jmxConnector;
    MBeanServerConnection mBeanServerConnection;
    private static final ObjectName HVP_MBEAN;
    private static final ObjectName COMPUTING_MBEAN;
    private static final ObjectName COLLECT_MBEAN;
    private int late = 0;

    static {
        ObjectName MBEAN;
        try {
            MBEAN = new ObjectName("com.systar:type=com.systar.hvp.simulator.HvpSimulatorMXBean,name=hvp-simulator.hvpSimulator");
        } catch (MalformedObjectNameException e) {
            MBEAN = null;
        }
        HVP_MBEAN = MBEAN;
        try {
            MBEAN = new ObjectName("com.systar:type=com.systar.krypton.scheduler.impl.executor.ComputingExecutorMXBean,name=krypton-scheduler.computingExecutor");
        } catch (MalformedObjectNameException e) {
            MBEAN = null;
        }
        COMPUTING_MBEAN = MBEAN;
        try {
            MBEAN = new ObjectName("com.systar:type=com.systar.krypton.scheduler.impl.collector.CollectorMXBean,name=krypton-scheduler.defaultCollectorInterceptorFactory");
        } catch (MalformedObjectNameException e) {
            MBEAN = null;
        }
        COLLECT_MBEAN = MBEAN;
    }

    void bindControls(AtomicBoolean running) {
        this.running = running;
        playIcon = (FontIcon)playButton.getGraphic();
        pauseIcon = new FontIcon("ci-pause-filled");
        pauseIcon.setIconColor(playIcon.getIconColor());
        pauseIcon.setIconSize(playIcon.getIconSize());
        playButton.setGraphic(pauseIcon);
        // time series
        memSeries = new XYChart.Series<>();
        memSeries.setName("computing tasks");
        timeChart.getData().add(memSeries);
        timeChart.getXAxis().setTickLabelsVisible(false);
        timeChart.getXAxis().setTickMarkVisible(false);
        // Pie
        memPie.getData().clear();
        memPie.setLegendVisible(false);
        computingLabel.setText("Recomputing ratio");
        sliceLive = new PieChart.Data("Live", 0);
        sliceRecompute = new PieChart.Data("Recompute", 0);
        memPie.getData().add(sliceLive);
        memPie.getData().add(sliceRecompute);
        sliceLive.getNode().setStyle("-fx-pie-color: #2fff2f;");
        sliceRecompute.getNode().setStyle("-fx-pie-color: #ff5700;");
        // Stack
        channelStack.getData().clear();
        XYChart.Series<String, Number> dataSeries1 = new XYChart.Series<>();
        dataSeries1.setName("Tasks");
        channelStack.getData().add(dataSeries1);
        COMPUTES.forEach(label -> {
            XYChart.Data<String, Number> liveData = new XYChart.Data<>(label, 0);
            dataSeries1.getData().add(liveData);
            channelData.put(label, liveData);
        });
        // JMX
        try {
            JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://A-HM7KVT3.wks.ptx.axway.int:1999/jmxrmi");
            jmxConnector = JMXConnectorFactory.connect(jmxServiceURL);
            mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            late = (Integer) mBeanServerConnection.getAttribute(HVP_MBEAN, "LatePayments");
            lateLabel.setText(late + " mn");
        } catch (IOException | JMException e) {
            e.printStackTrace();
        }
        launchJMX();
        clearButton.setDisable(true);
    }

    private void launchJMX() {
        new Thread(() -> {
            try {
                while (running.get()) {
                    if (playing.get()) {
                        String now = TimeUtils.formatNow();
                        CompositeData[] collectStats = (CompositeData[]) mBeanServerConnection.getAttribute(COLLECT_MBEAN, "CollectStats");
                        CompositeData[] computingStats = (CompositeData[]) mBeanServerConnection.getAttribute(COMPUTING_MBEAN, "ComputingStats");
                        int liveCount = (Integer) mBeanServerConnection.getAttribute(COMPUTING_MBEAN, "LiveCount");
                        int correctionCount = (Integer) mBeanServerConnection.getAttribute(COMPUTING_MBEAN, "CorrectionCount");
                        updateComputing(now, liveCount, correctionCount);
                        Platform.runLater(() -> {
                            ObservableList<Map<String, Object>> absorbedItems = absorbed.getItems();
                            absorbedItems.clear();
                            absorbedItems.addAll(Arrays.stream(collectStats).map(ComputingController::toCollectItem).collect(Collectors.toList()));
                            ObservableList<Map<String, Object>> computedItems = computed.getItems();
                            computedItems.clear();
                            computedItems.addAll(Arrays.stream(computingStats).map(ComputingController::toComputeItem).collect(Collectors.toList()));
                        });
                    }

                    sleep(1_000);
                }

            } catch (IOException | JMException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static Map<String, Object> toCollectItem(CompositeData data) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", data.get("name"));
        item.put("lateCount", data.get("lateCount"));
        item.put("actual", data.get("actual"));
        item.put("detected", data.get("detected"));
        return item;
    }

    private static Map<String, Object> toComputeItem(CompositeData data) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", data.get("name"));
        Integer liveCount = (Integer)data.get("liveCount");
        item.put("liveCount", liveCount);
        Integer correctionCount = (Integer)data.get("correctionCount");
        item.put("correctionCount", correctionCount);

        int tasks = liveCount + correctionCount;
        int recomputingRatio = (tasks > 0) ? (int) Math.round(100.0 * correctionCount / tasks) : 0;
        item.put("ratio", String.format("%d %%", recomputingRatio));
        return item;
    }

    public void updateComputing(String time, int live, int correction) {

        int tasks = live + correction;
        int recomputingRatio = (tasks > 0) ? (int) Math.round(100.0 * correction / tasks) : 0;
        Platform.runLater(() -> {
            try {
                // time
                timeLabel.setText(time);
                ObservableList<XYChart.Data<String, Number>> timeSerie = memSeries.getData();
                int size = timeSerie.size();
                timeSerie.add(new XYChart.Data<>(Integer.toString(size), tasks));
                // ratio
                computingValue.setText(String.format("%d %%", recomputingRatio));
                // pie
                sliceLive.setPieValue(live);
                sliceRecompute.setPieValue(correction);
                // bars
                channelData.get(COMPUTES.get(0)).setYValue(live);
                channelData.get(COMPUTES.get(1)).setYValue(correction);
            } catch (IllegalArgumentException e) {
                // skip
            }
        });
    }

    void resetComputing() {
        timeLabel.setText("-");
        sliceLive.setPieValue(0);
        sliceRecompute.setPieValue(0);
        computingValue.setText("-");
        channelData.values().forEach(data -> data.setYValue(0));
    }

    public void onPlay(ActionEvent actionEvent) {
        boolean playing = !this.playing.get();
        this.playing.set(playing);
        playButton.setGraphic(playing ? pauseIcon : playIcon);
        clearButton.setDisable(playing);
        actionEvent.consume();
    }

    public void onClear(ActionEvent actionEvent) {
        memSeries.getData().clear();
        absorbed.getItems().clear();
        computed.getItems().clear();
        resetComputing();
        try {
            mBeanServerConnection.invoke(COMPUTING_MBEAN, "resetCounts", new Object[]{}, new String[]{});
            mBeanServerConnection.invoke(COLLECT_MBEAN, "resetCounts", new Object[]{}, new String[]{});
        } catch (JMException | IOException e) {
            e.printStackTrace();
        }

        actionEvent.consume();
    }

    public void onAcknowledge(ActionEvent actionEvent) {
        try {
            mBeanServerConnection.invoke(COLLECT_MBEAN, "acknowledgeComputingStats", new Object[]{}, new String[]{});
        } catch (JMException | IOException e) {
            e.printStackTrace();
        }

        actionEvent.consume();
    }

    public void onLess(ActionEvent actionEvent) {
        try {
            if (late > 0) {
                late--;
                mBeanServerConnection.setAttribute(HVP_MBEAN, new Attribute("LatePayments", late));
                lateLabel.setText(late + " mn");
            }
        } catch (JMException | IOException e) {
            e.printStackTrace();
        }

        actionEvent.consume();
    }

    public void onMore(ActionEvent actionEvent) {
        try {
            late++;
            mBeanServerConnection.setAttribute(HVP_MBEAN, new Attribute("LatePayments", late));
            lateLabel.setText(late + " mn");
        } catch (JMException | IOException e) {
            e.printStackTrace();
        }

        actionEvent.consume();
    }
}
