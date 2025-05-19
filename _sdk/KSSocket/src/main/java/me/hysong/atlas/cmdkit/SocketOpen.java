package me.hysong.atlas.cmdkit;

import lombok.Getter;
import me.hysong.atlas.cmdkit.objects.KSScriptingNull;
import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.kssocket.v1.KSSocket;
import me.hysong.atlas.sharedobj.KSExecutionSession;

import java.util.List;

@Getter
public class SocketOpen implements KSScriptingExecutable {

    private final String manual = "SocketOpen@me.hysong.atlas.cmdkit\n" +
            "This command will open a socket server that echos the incoming data." +
            "Usage: SocketOpen <listening port> <whitelist> [optional: authorization if KSSocket requires]";

    @Override
    public String returnType() {
        return KSScriptingNull.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Check parameters
        if (args.length < 2) {
            throw new RuntimeException("Usage: SocketOpen <listening port> <whitelist> [optional: authorization if KSSocket requires]");
        }

        Object port = args[0];
        List<String> whitelist;

        if (args[1] instanceof List<?>) {
            whitelist = (List<String>) args[1];
        } else {
            throw new RuntimeException("Usage: SocketOpen <listening port> <whitelist> [optional: authorization if KSSocket requires]");
        }

        int realPort = -1;
        // Check if port is a number
        if (Number.class.isAssignableFrom(port.getClass())) {
            realPort = (int) port;
        } else {
            try {
                realPort = Integer.parseInt(port.toString());
            } catch (Exception e) {
                throw new RuntimeException("Port must be a number");
            }
        }

        KSSocket ssrv = new KSSocket(whitelist.toArray(new String[0]), realPort, (request, decodedPayload) -> null);
        ssrv.run();
        return new KSScriptingNull();
    }
}
