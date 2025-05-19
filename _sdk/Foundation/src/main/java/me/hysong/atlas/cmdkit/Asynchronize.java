package me.hysong.atlas.cmdkit;

import lombok.Getter;
import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sharedobj.KSExecutionSession;
import me.hysong.atlas.utils.KSScriptingInterpreter;

@Getter
public class Asynchronize implements KSScriptingExecutable {

    private final boolean preprocessingInterpreterWhitelistEnabled = true;
    private final int[] preprocessingInterpreterWhitelist = new int[]{};

    @Override
    public String returnType() {
        return Thread.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Usage:
        //   Asynchronize <code>
        if (args == null || args.length < 1) {
            throw new RuntimeException("Asynchronize requires at least one command");
        }

        Thread running = new Thread(() -> {
            KSScriptingInterpreter.execute(args, session);
        });
        running.start();
        return running;
    }
}
