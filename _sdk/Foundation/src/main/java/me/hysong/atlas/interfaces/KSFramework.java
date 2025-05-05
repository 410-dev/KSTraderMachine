package me.hysong.atlas.interfaces;

import me.hysong.atlas.sharedobj.KSEnvironment;

public interface KSFramework {
    int frameworkMain(KSEnvironment environment, String execLocation, String[] args);
}
