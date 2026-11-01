package util;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * An extended {@link JButton} that preserves a reference to its primary {@link ActionListener}.
 * Widely used across the menu system for fluent, concise button initialization.
 */
public class JButtonListener extends JButton {
    private final ActionListener buttonAction;

    public JButtonListener() {
        this(null, null, null);
    }

    public JButtonListener(ActionListener buttonAction) {
        this(null, null, buttonAction);
    }

    public JButtonListener(Icon icon, ActionListener buttonAction) {
        this(null, icon, buttonAction);
    }

    public JButtonListener(String text, ActionListener buttonAction) {
        this(text, null, buttonAction);
    }

    public JButtonListener(String text, Icon icon, ActionListener buttonAction) {
        super(text, icon);
        if (buttonAction != null) {
            addActionListener(buttonAction);
        }
        this.buttonAction = buttonAction;
    }

    /**
     * Returns the {@link ActionListener} associated with this button.
     */
    public ActionListener buttonAction() {
        return buttonAction;
    }
}
