package nl.obren.sokrates.common.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessingStopwatch {
    private static final Log LOG = LogFactory.getLog(ProcessingStopwatch.class);

    private static List<ProcessingTimes> monitors = new ArrayList<>();
    private static Map<String, ProcessingTimes> monitorsMap = new HashMap<>();

    public static void start(String processingName) {
        ProcessingTimes times = new ProcessingTimes(processingName);

        monitors.add(times);
        monitorsMap.put(processingName, times);

        LOG.info("Starting " + processingName);
    }

    public static void end(String processingName) {
        if (monitorsMap.containsKey(processingName)) {
            ProcessingTimes times = monitorsMap.get(processingName);
            times.end();
            LOG.info("Done '" + times.getProcessing() + "' in " + times.getDuration() + "ms");
        } else {
            LOG.error("No processing with the name '" + processingName + "'");
        }
    }

    public static List<ProcessingTimes> getMonitors() {
        return monitors;
    }

    public static void print() {
        LOG.info("Processing times summary:");
        monitors.forEach(monitor -> {
            LOG.info("Executed '" + monitor.getProcessing() + "' in " + monitor.getDuration() + "ms");
        });
    }
}

class ProcessingTimes {
    private String processing = "";
    private long start;
    private long end;

    public ProcessingTimes(String processing) {
        this.processing = processing;
        this.start();
    }

    public void start() {
        this.start = System.currentTimeMillis();
        this.end = 0;
    }

    public void end() {
        this.end = System.currentTimeMillis();
    }

    public long getDuration() {
        return end - start;
    }

    public String getProcessing() {
        return processing;
    }

    public void setProcessing(String processing) {
        this.processing = processing;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }
}