package me.hysong.atlas.sdk.graphite.v1;

import javax.swing.*;
import java.util.HashMap;

public class GraphiteWindowServer {
    public static HashMap<String, JFrame> windows = new HashMap<>();
    public static HashMap<String, Integer> incrementalWindowIDs = new HashMap<>();

    public static JFrame makeWindow(String windowName) {
        JFrame window = new JFrame(windowName);
        if (windows.containsKey(windowName)) {
            int windowID = incrementalWindowIDs.getOrDefault(windowName, -1) + 1;
            incrementalWindowIDs.put(windowName, windowID);
        } else {
            incrementalWindowIDs.put(windowName, 0);
        }
        String windowID = windowName + ":" + incrementalWindowIDs.get(windowName);
        windows.put(windowID, window);
        return window;
    }
}
