package me.hysong.atlas.cmdkit;

import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sdk.graphite.v1.GPGenericWindow;
import me.hysong.atlas.sharedobj.KSExecutionSession;

import java.awt.*;

public class GraphiteGetElementByID implements KSScriptingExecutable {

    @Override
    public String returnType() {
        return Object.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Usage example: GraphiteGetElementByID {GPGenericWindow Object} username
        // Usage        : GraphiteGetElementByID {GPGenericWindow Object} <element id>
        if (args == null || args.length != 2) {
            throw new RuntimeException("GraphiteGetElement requires 2 arguments: {GPGenericWindowObject} <element id>");
        }

        if (!(args[0] instanceof GPGenericWindow)) {
            throw new RuntimeException("First argument must be a GPGenericWindow object.");
        }

        GPGenericWindow window = (GPGenericWindow) args[0];
        String elementId = (String) args[1];
        Component component = window.getComponentById(elementId);
        if (component == null) {
            throw new RuntimeException("Element with ID " + elementId + " not found in loaded program.");
        } else {
            return component;
        }
    }
}
