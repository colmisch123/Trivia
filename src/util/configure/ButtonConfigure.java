package util.configure;

import screen.Screen;

import javax.swing.*;
import java.awt.*;

/**
 * Fluent builder for creating and attaching {@link JButton} components
 * to a {@link JPanel} managed by a {@link GridBagLayout}.
 *
 * <p>Example:
 * <pre>{@code
 * ButtonConfigure.configure()
 *     .setX(0).setY(1)
 *     .setWidth(100).setHeight(30)
 *     .setBackgroundColor(Screen.JEOPARDY_BLUE)
 *     .setForegroundColor(Screen.JEOPARDY_YELLOW)
 *     .finish(button, screen, panel);
 * }</pre>
 * </p>
 */
public class ButtonConfigure extends BaseComponentConfigure<ButtonConfigure> {
    private Color foregroundColor;
    private String text;

    public static ButtonConfigure configure() {
        return new ButtonConfigure();
    }

    public ButtonConfigure setForegroundColor(Color color) {
        this.foregroundColor = color;
        return this;
    }

    public ButtonConfigure setForegroundColor(int red, int green, int blue) {
        int r = Math.min(255, Math.max(0, red));
        int g = Math.min(255, Math.max(0, green));
        int b = Math.min(255, Math.max(0, blue));
        this.foregroundColor = new Color(r, g, b);
        return this;
    }

    public ButtonConfigure setText(String text) {
        this.text = text;
        return this;
    }

    /**
     * Finishes configuring the button and adds it to the target panel with GridBagConstraints.
     */
    public void finish(JButton button, Screen screen, JPanel panel) {
        finish(button, panel, screen.getLayout(), GridBagConstraints.CENTER);
    }

    public void finish(JButton button, Screen screen, JPanel panel, int gridBagAlignment) {
        finish(button, panel, screen.getLayout(), gridBagAlignment);
    }

    public void finish(JButton button, JPanel panel, GridBagLayout layout) {
        finish(button, panel, layout, GridBagConstraints.CENTER);
    }

    public void finish(JButton button, JPanel panel, GridBagLayout layout, int gridBagAlignment) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.ipadx = width;
        gbc.ipady = height;
        gbc.insets = createInsets();
        gbc.anchor = gridBagAlignment;

        layout.setConstraints(button, gbc);
        panel.add(button);
        button.setVisible(visible);

        if (backgroundColor != null) {
            button.setBackground(backgroundColor);
        }
        if (foregroundColor != null) {
            button.setForeground(foregroundColor);
        }
        if (text != null) {
            button.setText(text);
        }
    }
}
