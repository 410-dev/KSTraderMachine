package me.hysong.atlas.interfaces;

import me.hysong.atlas.sharedobj.KSEnvironment;

public interface KSApplication {
    default boolean isMultipleInstancesAllowed() {return false;}
    String getAppDisplayName();
    int appMain(KSEnvironment environment, String execLocation, String[] args);
}
