package me.hysong.atlas.cmdkit;

import me.hysong.atlas.cmdkit.objects.KSScriptingNull;
import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sharedobj.KSExecutionSession;

import java.util.Map;

public class DropItemFromMap implements KSScriptingExecutable {
    @Override
    public String returnType() {
        return Map.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Usage: DropItemFromMap <map> <key>
        if (args.length != 2) {
            throw new IllegalArgumentException("DropItemFromMap requires exactly two arguments.");
        }

        Object map = args[0];
        Object key = args[1];

        if (KSScriptingNull.isNull(map)) {
            throw new IllegalArgumentException("The first argument cannot be null.");
        }

        if (map instanceof Map) {

            // Make a copy of the map to avoid modifying the original
            Map<Object, Object> newMap = ((Map<Object, Object>) map).getClass().newInstance();
            newMap.putAll((Map<Object, Object>) map);

            // Add the new key-value pair to the copied map
            newMap.remove(key);
            return newMap; // Return the modified map
        } else {
            throw new IllegalArgumentException("The first argument must be a Map.");
        }
    }
}
