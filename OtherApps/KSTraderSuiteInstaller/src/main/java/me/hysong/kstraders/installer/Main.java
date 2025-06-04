package me.hysong.kstraders.installer;

import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.GraphiteProgramLauncher;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;

import java.util.ArrayList;
import java.util.HashMap;


public class Main extends KSGraphicalApplication implements KSApplication {

    private final ArrayList<String> scenesThrough = new ArrayList<>();
    private final HashMap<String, InstallerScene> scenes = new HashMap<>();

    public static void main(String[] args) {
        GraphiteProgramLauncher.launch(Main.class, args);
    }

    @Override
    public String getAppDisplayName() {
        return "KSTraderSuiteInstaller";
    }

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        return 0;
    }

    @Override
    public int getWindowWidth() {
        return 800;
    }

    @Override
    public int getWindowHeight() {
        return 600;
    }
}