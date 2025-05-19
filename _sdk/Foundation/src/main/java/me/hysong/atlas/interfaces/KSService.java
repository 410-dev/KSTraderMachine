package me.hysong.atlas.interfaces;

import me.hysong.atlas.sharedobj.KSEnvironment;

public interface KSService {
    int serviceMain(KSEnvironment environment, String execLocation, String[] args);
    int stop(int code);
}
