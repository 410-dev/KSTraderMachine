package me.hysong.atlas.sdk.graphite.v1;


import java.awt.*;

public interface GPGenericWindow {
    void setTitle(String title);
    void setContentPane(Component content);
    void pack();
    void setVisible(boolean visible);
    void titleBar(boolean visible);
    void dispose();
    boolean isVisible();

    void setSize(int width, int height);

    void setLocationRelativeTo(Object o);

    void setLocation(int x, int y);

    void setCursor(Cursor predefinedCursor);

    void setType(Window.Type type);

    void setFocusable(boolean b);

    void setFocusableWindowState(boolean b);

    void setLayout(BorderLayout borderLayout);

    void setOpacity(float v);

    void setBackground(Color color);

    void setAlwaysOnTop(boolean b);

    void setDefaultCloseOperation(int disposeOnClose);

    void setResizable(boolean b);

    void setIconImage(Image image);

    Component getComponentById(String elementId);
}
