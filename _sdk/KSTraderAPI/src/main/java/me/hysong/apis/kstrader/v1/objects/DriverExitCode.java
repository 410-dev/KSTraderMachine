package me.hysong.apis.kstrader.v1.objects;

public enum DriverExitCode {


    // OK,
    OK,
    DRIVER_TEST_OK,
    DRIVER_TERMINATED_INTENTIONALLY,

    // Driver internal issues
    DRIVER_UNEXPECTED_ERROR,
    DRIVER_BAD_DATA_ERROR,

    // Driver external issues
    DATA_MISSING_ERROR,

    // Server issue
    SERVER_UNEXPECTED_ERROR,

    // Server connection issues
    SERVER_CONNECTION_ERROR,
}
