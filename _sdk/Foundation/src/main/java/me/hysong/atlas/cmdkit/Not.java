package me.hysong.atlas.cmdkit;

import me.hysong.atlas.cmdkit.types.Bool;
import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sharedobj.KSExecutionSession;

public class Not implements KSScriptingExecutable {
    @Override
    public String returnType() {
        return Bool.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Usage: Not <condition>
        if (args == null || args.length != 1) {
            throw new RuntimeException("Not requires 1 argument");
        }

        Object arg = args[0];
        if (arg instanceof Boolean) {
            return !(Boolean) arg;
        } else if (arg instanceof String) {
            return !Boolean.parseBoolean((String) arg);
        } else {
            throw new RuntimeException("Not requires a boolean or a string that can be parsed to boolean");
        }
    }
}
