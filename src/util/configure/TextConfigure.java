package util.configure;

import screen.Screen;

import javax.swing.*;
import java.awt.*;

/**
 * Fluent builder for creating and attaching {@link JLabel} text components
 * to a {@link JPanel} managed by a {@link GridBagLayout}.
 *
 * <p>Example:
 * <pre>{@code
 * TextConfigure.configure()
 *     .setText("Categories")
 *     .setTextColor(Screen.JEOPARDY_YELLOW)
 *     .setBackgroundColor(Screen.JEOPARDY_BLUE)
 *     .setSize(24).setStyle(Font.BOLD)
 *     .confirm(label, screen, panel);
 * }</pre>
 * </p>
 */
public class TextConfigure extends BaseComponentConfigure<TextConfigure> {
    private String text = "";
    private Color textColor = Color.WHITE;

    public static TextConfigure configure() {
        return new TextConfigure();
    }

    public TextConfigure setText(String text) {
        this.text = text != null ? text : "";
        return this;
    }

    public TextConfigure setTextColor(Color color) {
        this.textColor = color;
        return this;
    }

    public TextConfigure setTextColor(int red, int green, int blue) {
        int r = Math.min(255, Math.max(0, red));
        int g = Math.min(255, Math.max(0, green));
        int b = Math.min(255, Math.max(0, blue));
        this.textColor = new Color(r, g, b);
        return this;
    }

    /**
     * Applies the configuration to the label and adds it to the target panel.
     */
    public void confirm(JLabel label, Screen screen, JPanel panel) {
        confirm(label, screen, panel, GridBagConstraints.CENTER);
    }

    public void confirm(JLabel label, Screen screen, JPanel panel, int layoutAnchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.ipadx = width;
        gbc.ipady = height;
        gbc.insets = createInsets();
        gbc.anchor = layoutAnchor;

        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setText(text);
        label.setForeground(textColor);
        label.setFont(createFont());

        screen.getLayout().setConstraints(label, gbc);
        panel.add(label);
        label.setVisible(visible);

        if (backgroundColor != null) {
            label.setOpaque(true);
            label.setBackground(backgroundColor);
        }
    }
}
