package me.hysong.atlas.sdk.graphite.v1.subcomponents.quirks;

import javax.swing.*;
import java.awt.event.MouseListener;

public class GPButton extends JButton {
    public GPButton() {
        super();

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Custom behavior on click
                System.out.println("Button clicked!");
            }
        });
    }

    public void onClick() {
        for (MouseListener mouseListener : getMouseListeners()) {
            if (mouseListener instanceof java.awt.event.MouseAdapter) {
                mouseListener.mouseClicked(null);
            }
        }
    }
}
