package me.hysong.kstraders.installer;

import javax.swing.*;

public abstract class InstallerScene extends JPanel {
    public abstract Object getValueOf(String key, Object defaultValue);
    public abstract InstallerScene next();
    public abstract String getSceneId();
    public void reload(){}
    public boolean isLoadingScreen() {return false;}
    public Runnable getBackendRunnable() {return null;}
}
