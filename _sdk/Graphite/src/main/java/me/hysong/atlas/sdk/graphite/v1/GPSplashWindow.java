package me.hysong.atlas.sdk.graphite.v1;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

@Getter
public class GPSplashWindow extends JPanel {
    @Setter private Thread splashBackend;
    @Setter private String imageLocation;
    @Setter private String statusSuffix = "";
    @Setter private String statusSuffixSpacing = "";
    @Setter private String statusPrefix = "";
    @Setter private String statusPrefixSpacing = "";
    private String currentStatus;
    private final int width;
    private final int height;
    private final int alignment;
    @Setter private Color backgroundColor;
    @Setter private Color foregroundColor;

    public GPSplashWindow(int width, int height, int alignment) {
        this.width = width;
        this.height = height;
        this.alignment = alignment;
        this.currentStatus = "Starting up";

        setSize(width, height);
    }

    public void setCurrentStatus(String currentStatus) {
        System.out.println("Current status: " + currentStatus);
        this.currentStatus = statusPrefix + statusPrefixSpacing + currentStatus + statusSuffixSpacing + statusSuffix;
    }
}
