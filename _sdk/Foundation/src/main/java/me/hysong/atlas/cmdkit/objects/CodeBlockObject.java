package me.hysong.atlas.cmdkit.objects;

import lombok.Getter;
import me.hysong.atlas.sharedobj.KSExecutionSession;
import me.hysong.atlas.utils.KSScriptingInterpreter;

import java.util.ArrayList;
import java.util.HashMap;

@Getter
public class CodeBlockObject {

    private final String name;
    private final ArrayList<String> line;

    public CodeBlockObject(String name) {
        this.name = name;
        this.line = new ArrayList<>();
    }

    public void addCodeLine(String code) {
//        System.out.println("Adding code line: " + code);
        line.add(code);
    }

    public Object run(KSExecutionSession session) {
//        System.out.println("Running code block: " + name);
        String[] lines = line.toArray(new String[0]);
        try {
            return KSScriptingInterpreter.executeLines(lines, session);
        } catch (Exception e) {
            throw new RuntimeException("Error executing code block: " + name, e);
        }
    }
}
