package me.hysong.atlas.cmdkit;

import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sharedobj.KSExecutionSession;

public class GetClassForName implements KSScriptingExecutable {

    @Override
    public String returnType() {
        return Class.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Usage:
        //   GetClassForName <class name>

        // Check if the first argument is a class name
        if (args == null || args.length < 1) {
            throw new RuntimeException("GetClassForName requires at least 1 argument: <class name>");
        }

        String className = (String) args[0];
        return Class.forName(className);
    }
}
