package me.hysong.atlas.cmdkit;

import me.hysong.atlas.cmdkit.objects.KSScriptingNull;
import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sharedobj.KSExecutionSession;

public class Exit implements KSScriptingExecutable {
    @Override
    public String returnType() {
        return KSScriptingNull.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        session.setSessionTerminated(true);
        session.setTerminatingValue(args.length > 0 ? args[0] : new KSScriptingNull());
        return session.getTerminatingValue();
    }
}
