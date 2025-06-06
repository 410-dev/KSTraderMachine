package me.hysong.kstraders.installer.scenes;

import me.hysong.kstraders.installer.InstallerScene;

import javax.swing.*;
import java.awt.*;

public class Introduction extends InstallerScene {

    public Introduction() {
        JLabel title = new JLabel("KSTraderSuite Installer");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setForeground(Color.BLACK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setVerticalAlignment(SwingConstants.CENTER);

        JLabel button = new JLabel("Next");
    }

    @Override
    public Object getValueOf(String key, Object defaultValue) {
        return defaultValue;
    }

    @Override
    public InstallerScene next() {
        return new EULA();
    }

    @Override
    public String getSceneId() {
        return "Introduction";
    }
}
