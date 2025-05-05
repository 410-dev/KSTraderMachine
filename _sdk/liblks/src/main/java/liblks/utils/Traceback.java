package liblks.utils;

public class Traceback {

    public static String getCallerClassName(int offset) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > offset + 2) {
            return stackTrace[offset + 2].getClassName();
        }
        return null;
    }

    public static String getCallerMethodName(int offset) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > offset + 2) {
            return stackTrace[offset + 2].getMethodName();
        }
        return null;
    }

    public static String getCallerClassName() {
        return getCallerClassName(0);
    }

}
