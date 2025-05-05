package me.hysong.atlas.cmdkit;

import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sdk.graphite.v1.GPGenericWindow;
import me.hysong.atlas.sharedobj.KSExecutionSession;

import java.util.HashMap;

public class GraphiteGetWindow implements KSScriptingExecutable {
    @Override
    public String returnType() {
        return GPGenericWindow.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Usage: GraphiteGetWindow <program class>:<window id>
        if (args == null || args.length != 1) {
            throw new RuntimeException("GraphiteGetWindow requires 1 argument: <program class>:<window id>");
        }

        String windowAddress = (String) args[0];

        // Obtain the components
        String[] elementAddress = windowAddress.split(":");
        if (elementAddress.length != 2) {
            throw new RuntimeException("Invalid element address format. Expected format: <program class>:<window id>:<component class>:<element id>");
        }
        String programClass = elementAddress[0];
        String windowId = elementAddress[1];

        // Using reflection, get private static hashmap from GPComponentFactory.
        HashMap<String, GPGenericWindow> windows;
        HashMap<String, Integer> incrementalWindowIDs;
        try {
            Class<?> componentFactoryClass = Class.forName("me.hysong.atlas.sdk.graphite.v1.factories.GPComponentFactory");
            java.lang.reflect.Field windowsField = componentFactoryClass.getDeclaredField("windows");
            java.lang.reflect.Field incrementalWindowIDsField = componentFactoryClass.getDeclaredField("incrementalWindowIDs");
            windowsField.setAccessible(true);
            incrementalWindowIDsField.setAccessible(true);
            windows = (HashMap<String, GPGenericWindow>) windowsField.get(null);
            incrementalWindowIDs = (HashMap<String, Integer>) incrementalWindowIDsField.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access GPComponentFactory.windows", e);
        }

        // Get the window
        if (windowId == null || windowId.equals("?")) {
            windowId = String.valueOf(incrementalWindowIDs.get(programClass));
        }

        return windows.get(programClass + ":" + windowId);
    }
}
