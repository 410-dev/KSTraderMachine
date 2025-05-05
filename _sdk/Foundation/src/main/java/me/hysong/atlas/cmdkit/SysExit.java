package me.hysong.atlas.cmdkit;

import me.hysong.atlas.cmdkit.objects.KSScriptingNull;
import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sharedobj.KSExecutionSession;

public class SysExit implements KSScriptingExecutable {
    @Override
    public String returnType() {
        return KSScriptingNull.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Usage:
        //   SysExit
        //   SysExit 0

        if (args.length > 0) {
            switch (args[0]) {
                case Integer i -> System.exit(i);
                case Long l -> System.exit(l.intValue());
                case String s -> {
                    try {
                        System.exit(Integer.parseInt((String) args[0]));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid argument: " + args[0]);
                    }
                }
                case null, default ->
                        System.exit(0);
            }
        }
        return new KSScriptingNull();
    }
}
