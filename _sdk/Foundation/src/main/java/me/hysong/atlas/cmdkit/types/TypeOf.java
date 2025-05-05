package me.hysong.atlas.cmdkit.types;

import me.hysong.atlas.cmdkit.objects.KSScriptingNull;
import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sharedobj.KSExecutionSession;

public class TypeOf implements KSScriptingExecutable {
    @Override
    public String returnType() {
        return String.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Usage: TypeOf <object>

        if (args.length != 1) {
            throw new IllegalArgumentException("TypeOf requires exactly one argument.");
        }

        Object arg = args[0];
        if (KSScriptingNull.isNull(arg)) {
            return "null";
        }

        return arg.getClass().getName();
    }
}
