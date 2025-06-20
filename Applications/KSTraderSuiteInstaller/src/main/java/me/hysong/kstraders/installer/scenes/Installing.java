package me.hysong.kstraders.installer.scenes;

import me.hysong.kstraders.installer.InstallerScene;

public class Installing extends InstallerScene {

    private boolean success = false;

    @Override
    public Object getValueOf(String key, Object defaultValue) {
        return null;
    }

    @Override
    public InstallerScene next() {
        return new Complete(success);
    }

    @Override
    public String getSceneId() {
        return "";
    }

    @Override
    public boolean isLoadingScreen() {
        return true;
    }

    public Runnable getBackendRunnable() {
        return () -> {
            success = true;
        };
    }
}
