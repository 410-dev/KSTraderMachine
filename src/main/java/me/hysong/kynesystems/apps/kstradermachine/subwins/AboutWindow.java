package me.hysong.kynesystems.apps.kstradermachine.subwins;

import lombok.Getter;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.GPSplashWindow;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;

import javax.swing.*;

@Getter
public class AboutWindow extends KSGraphicalApplication implements KSApplication {
    private final String appDisplayName = "About KSTraderMachine";
    private final int closeBehavior = JFrame.DISPOSE_ON_CLOSE;
    private final int width = 400;
    private final int height = 300;

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Current build: 2025-05-09-A");
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setVerticalAlignment(SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(20f));
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setAlignmentY(CENTER_ALIGNMENT);
        title.setSize(width, title.getFont().getSize());
        title.setLocation(0, 0);
        title.setVisible(true);
        add(title);
        return 0;
    }

}
