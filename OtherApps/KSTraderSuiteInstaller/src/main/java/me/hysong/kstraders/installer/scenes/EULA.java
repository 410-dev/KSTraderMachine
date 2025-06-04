package me.hysong.kstraders.installer.scenes;

import me.hysong.kstraders.installer.InstallerScene;

public class EULA extends InstallerScene {
    @Override
    public Object getValueOf(String key, Object defaultValue) {
        return null;
    }

    @Override
    public InstallerScene next() {
        return new Activation();
    }

    @Override
    public String getSceneId() {
        return "";
    }
}
