package me.hysong.atlas.sdk.graphite.v1.subcomponents.quirks;

import javax.swing.*;

public class GPPasswordField extends JPasswordField {
    public String getEnteredText() {
        return new String(super.getPassword());
    }
}
