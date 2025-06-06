package me.hysong.kstraders.installer.scenes;

import me.hysong.kstraders.installer.InstallerScene;

public class Complete extends InstallerScene {

    public Complete(boolean success) {}

    @Override
    public Object getValueOf(String key, Object defaultValue) {
        return null;
    }

    @Override
    public InstallerScene next() {
        return null;
    }

    @Override
    public String getSceneId() {
        return "";
    }
}
