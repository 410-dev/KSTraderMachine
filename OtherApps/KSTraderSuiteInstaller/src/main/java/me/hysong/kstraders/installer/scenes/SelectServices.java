package me.hysong.kstraders.installer.scenes;

import me.hysong.kstraders.installer.InstallerScene;

public class SelectServices extends InstallerScene {
    @Override
    public Object getValueOf(String key, Object defaultValue) {
        return null;
    }

    @Override
    public InstallerScene next() {
        return new SelectPrograms();
    }

    @Override
    public String getSceneId() {
        return "";
    }
}
