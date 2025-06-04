package me.hysong.kstraders.installer.scenes;

import me.hysong.kstraders.installer.InstallerScene;

public class InstallationConfirm extends InstallerScene {

    @Override
    public Object getValueOf(String key, Object defaultValue) {
        return null;
    }

    @Override
    public InstallerScene next() {
        return new Installing();
    }

    @Override
    public String getSceneId() {
        return "";
    }
}
