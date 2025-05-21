package me.hysong.atlas.sdk.graphite.v1;

import javax.swing.*;
import java.awt.*;

public abstract class KSGraphicalApplication extends JPanel{
    public GPSplashWindow getSplashWindow() {return null;}

    public abstract int getWindowWidth();
    public abstract int getWindowHeight();

    public int getCloseBehavior() {return JFrame.EXIT_ON_CLOSE;}
    public boolean isResizable() {return true;}
    public boolean isAlwaysOnTop() {return false;}
    public boolean isUndecorated() {return false;}
    public boolean isFullscreen() {return false;}
    public boolean isMaximized() {return false;}
    public float getOpacity() {return 1.0f;}
    public Image getAppIcon() {return null;}
    public Component getLocationRelativeTo() {return null;}
    public int getMaxFPS() {return 60;}
    public int getHardwareMaxFPS() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getDisplayMode().getRefreshRate();
    }
    public long getFPSTick() {
        return 1000 / Math.min(getMaxFPS(), getHardwareMaxFPS());
    }
    public boolean refreshByFPS() {
        return false;
    }
}
