package me.hysong.apis.kstrader.v1.objects;

import lombok.Getter;

import java.util.ArrayList;

@Getter
public class DriverState {

    @Getter
    private static final ArrayList<DriverState> driverStates = new ArrayList<>();

    private final DriverExitCode exitCode;
    private final String message;
    private final Exception exception;

    private DriverState(DriverExitCode exitCode, String message, Exception exception) {
        this.exitCode = exitCode;
        this.message = message;
        this.exception = exception;
    }

    public static void addState(DriverExitCode exitCode, String message) {
        addState(exitCode, message, null);
    }

    public static void addState(DriverExitCode exitCode, String message, Exception exception) {
        driverStates.add(new DriverState(exitCode, message, exception));

        if (exitCode != DriverExitCode.OK && exitCode != DriverExitCode.DRIVER_TERMINATED_INTENTIONALLY && exitCode != DriverExitCode.DRIVER_TEST_OK) {
            System.out.println("Driver state: " + exitCode + " - " + message);
            if (exception != null) {
                exception.printStackTrace();
            }
        } else if (exception != null) {
            System.out.println("Driver state: " + exitCode + " - " + message);
            exception.printStackTrace();
        }
    }

}
