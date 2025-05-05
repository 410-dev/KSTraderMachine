package me.hysong.atlas.cmdkit;

import me.hysong.atlas.cmdkit.types.Bool;
import me.hysong.atlas.interfaces.KSScriptingExecutable;
import me.hysong.atlas.sharedobj.KSExecutionSession;

public class CompareNumber implements KSScriptingExecutable {
    @Override
    public String returnType() {
        return Bool.class.getName();
    }

    @Override
    public Object execute(Object[] args, KSExecutionSession session) throws Exception {
        // Usage:
        //   CompareNumber <number1> <operator> <number2>
        //   operator: ==, !=, <, <=, >, >=
        if (args == null || args.length != 3) {
            throw new RuntimeException("CompareNumber requires 3 arguments: <number1> <operator> <number2>");
        }
        Object arg1 = args[0];
        Object arg2 = args[2];
        String operator = (String) args[1];
        if (arg1 instanceof Number && arg2 instanceof Number) {
            double num1 = ((Number) arg1).doubleValue();
            double num2 = ((Number) arg2).doubleValue();
            return compare(num1, operator, num2);
        } else {
            throw new RuntimeException("CompareNumber requires two numbers as arguments");
        }
    }

    public static boolean compare(double num1, String operator, double num2) {
        return switch (operator) {
            case "==" -> num1 == num2;
            case "!=" -> num1 != num2;
            case "<" -> num1 < num2;
            case "<=" -> num1 <= num2;
            case ">" -> num1 > num2;
            case ">=" -> num1 >= num2;
            default -> throw new RuntimeException("Invalid operator. Expected any of ==, !=, <, <=, >, >= but got: " + operator);
        };
    }
}
