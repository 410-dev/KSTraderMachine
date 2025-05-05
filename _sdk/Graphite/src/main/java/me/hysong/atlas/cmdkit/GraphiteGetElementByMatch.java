package me.hysong.atlas.cmdkit;

import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sharedobj.KSExecutionSession;

import java.awt.*;

public class GraphiteGetElementByMatch implements KSScriptingExecutable {
    @Override
    public String returnType() {
        return Object.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Usage example: GraphiteGetElementByMatch {GPGenericWindow Object} Type=javax.swing.JButton FilterBy=getText
        return null;
    }
}
