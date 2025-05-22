package me.hysong.kynesystems.apps.kstradermachine.subwins;

import lombok.Getter;
import me.hysong.atlas.interfaces.KSApplication;
import me.hysong.atlas.sdk.graphite.v1.KSGraphicalApplication;
import me.hysong.atlas.sharedobj.KSEnvironment;

import javax.swing.*;

@Getter
public class AboutWindow extends KSGraphicalApplication implements KSApplication {
    private final String appDisplayName = "About KSTraderMachine";
    private final int closeBehavior = JFrame.DISPOSE_ON_CLOSE;
    private final int windowWidth = 400;
    private final int windowHeight = 300;

    @Override
    public int appMain(KSEnvironment environment, String execLocation, String[] args) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        String[] linesToWrite = new String[]{
                "KSTraderMachine",
                "",
                "",
                "Version: 1.0",
                "Build: 25JN08A (2025. June 08 A)",
                "Release: Release",
                "Foundation: 1.0",
                "Graphite: 1.0",
                "Scripting mode: Disabled",
                "Telemetry mode: Disabled",
                "Resource on VFS: No"
        };

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        for (String line : linesToWrite) {
            sb.append(line);
            sb.append("<br>");
        }
        sb.append("</html>");

        JLabel text = new JLabel(sb.toString());
        text.setAlignmentX(CENTER_ALIGNMENT);
        text.setHorizontalAlignment(SwingConstants.LEFT);
        text.setVerticalAlignment(SwingConstants.CENTER);
        text.setFont(text.getFont().deriveFont(15f));
        text.setAlignmentX(CENTER_ALIGNMENT);
        text.setAlignmentY(CENTER_ALIGNMENT);
        text.setSize(windowWidth, text.getFont().getSize());
        text.setLocation(0, 0);
        text.setVisible(true);
        add(text);

        return 0;
    }

}
