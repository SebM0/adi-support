package com.axway.adi.tools.disturb.parsers;

import java.util.*;
import java.util.stream.*;

public class ThreadDump {
    public static final String THREAD_NAME_HEADER = "\"";
    public static final String THREAD_ID_HEADER = "\"";
    private static final String STATUS_HEADER = "java.lang.Thread.State:";
    private static final String TRACE_HEADER = "at ";
    private static final String LOCK_HEADER = "- ";
    private static final String SYSTAR = "com.systar.";
    private static final Set<String> UTILITIES = Set.of("tau", "gluon", "boson", "photon");
    public String header;
    public String name;
    public String id;
    public String status = "";
    public boolean idle = false;
    public final List<String> stack = new ArrayList<>();
    public final List<String> locks = new ArrayList<>();
    public final LinkedList<String> traversedComponents = new LinkedList<>();

    public ThreadDump(String header) {
        this.header = header;
        int pos = header.indexOf(THREAD_NAME_HEADER, 1);
        if (pos > 0)
            name = header.substring(1, pos);
        pos = header.indexOf(THREAD_ID_HEADER, pos);
        if (pos > 0) {
            pos += THREAD_ID_HEADER.length();
            int end = pos + 1;
            while (end < header.length() && !Character.isWhitespace(header.charAt(end))) {
                end++;
            }
            id = header.substring(pos, end).trim();
        }
    }

    void addStack(String code) {
        if (code.startsWith(STATUS_HEADER)) {
            status = code.substring(STATUS_HEADER.length()).trim();
        } else if (code.startsWith(TRACE_HEADER)) {
            String trace = code.substring(TRACE_HEADER.length());
            String component = getComponent(trace);
            if (component != null) {
                if (traversedComponents.isEmpty() || !component.equals(traversedComponents.getLast())) {
                    traversedComponents.add(component);
                }
            }
            stack.add(trace);
        } else if (code.startsWith(LOCK_HEADER)) {
            locks.add(code.substring(LOCK_HEADER.length()));
        }
    }

    public void aggregate() {
        idle = status.contains("WAITING") && (traversedComponents.isEmpty() //
                || (traversedComponents.size() == 1 && "calcium".equals(lastTraversed()) && name.contains("-asynchronousEventInterceptorHandlers.")) // asynchronous event threads idle
        );
    }

    private String lastTraversed() {
        return traversedComponents.isEmpty() ? "" : traversedComponents.getLast();
    }

    private String getComponent(String trace) {
        int pos = trace.indexOf(SYSTAR);
        if (pos == -1)
            return null;
        String component = trace.substring(pos + SYSTAR.length());
        pos = component.indexOf('.');
        if (pos > 0) {
            String subComponent = component.substring(pos + 1);
            component = component.substring(0, pos);
            // ignore utility components
            if (UTILITIES.contains(component))
                return null;
            pos = subComponent.indexOf('.');
            if (pos > 0) {
                subComponent = subComponent.substring(0, pos);
                if (!"impl".equals(subComponent) && Character.isLowerCase(subComponent.charAt(0))) {
                    component += "." + subComponent;
                }
            }
        }
        return component;
    }

    public String getStackTrace() {
        return String.join("\n", stack);
    }

    @Override
    public String toString() {
        String text = THREAD_NAME_HEADER + name + THREAD_NAME_HEADER;
        if (id != null) {
            text += " (" + id + ")";
        }
        if (!status.isEmpty()) {
            text += " - " + status;
        }
        if (!locks.isEmpty()) {
            text += "\nLocks:\n" + locks.stream().map(e -> LOCK_HEADER + e).collect(Collectors.joining("\n"));
        }
        if (!stack.isEmpty()) {
            text += "\nStackTrace:\n" + stack.stream().map(e -> TRACE_HEADER + e).collect(Collectors.joining("\n"));
        }
        return text;
    }
}
