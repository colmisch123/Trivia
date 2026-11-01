package util;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * Utility helper for quickly constructing a {@link JButton} with an attached {@link ActionListener}.
 */
public class JButtonUtil {
    public static JButton registerWithListener(ActionListener listener) {
        JButton button = new JButton();
        button.addActionListener(listener);
        return button;
    }
}
