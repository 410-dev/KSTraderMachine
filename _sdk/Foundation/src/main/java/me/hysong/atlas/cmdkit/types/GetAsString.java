package me.hysong.atlas.cmdkit.types;

import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sharedobj.KSExecutionSession;

public class GetAsString implements KSScriptingExecutable {

    @Override
    public String returnType() {
        return String.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        if (args == null || args.length < 1) {
            throw new RuntimeException("GetAsString requires at least 1 argument");
        }
        StringBuilder result = new StringBuilder();
        for (Object arg : args) {
            if (arg != null) {
                result.append(arg);
            }
        }
        return result.toString();
    }
}
