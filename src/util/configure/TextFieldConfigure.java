package util.configure;

import screen.Screen;

import javax.swing.*;
import java.awt.*;

/**
 * Fluent builder for creating and attaching {@link JTextField} text input components
 * to a {@link JPanel} managed by a {@link GridBagLayout}.
 *
 * <p>Example:
 * <pre>{@code
 * TextFieldConfigure.configure()
 *     .setText("Player 1")
 *     .setColumn(20)
 *     .setSize(15)
 *     .confirm(textField, screen, panel);
 * }</pre>
 * </p>
 */
public class TextFieldConfigure extends BaseComponentConfigure<TextFieldConfigure> {
    private String text = "";
    private Color textColor = Color.BLACK;
    private int column = 1;

    public static TextFieldConfigure configure() {
        return new TextFieldConfigure();
    }

    public TextFieldConfigure setText(String text) {
        this.text = text != null ? text : "";
        return this;
    }

    public TextFieldConfigure setTextColor(Color textColor) {
        this.textColor = textColor;
        return this;
    }

    public TextFieldConfigure setColumn(int column) {
        this.column = column;
        return this;
    }

    /**
     * Applies the configuration to the text field and adds it to the target panel.
     */
    public void confirm(JTextField textField, Screen screen, JPanel panel) {
        confirm(textField, screen, panel, GridBagConstraints.CENTER);
    }

    public void confirm(JTextField textField, Screen screen, JPanel panel, int layoutAnchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.ipadx = width;
        gbc.ipady = height;
        gbc.insets = createInsets();
        gbc.anchor = layoutAnchor;

        textField.setHorizontalAlignment(SwingConstants.CENTER);
        textField.setText(text);
        textField.setForeground(textColor);
        textField.setFont(createFont());
        textField.setColumns(column);

        screen.getLayout().setConstraints(textField, gbc);
        panel.add(textField);
        textField.setVisible(visible);

        if (backgroundColor != null) {
            textField.setBackground(backgroundColor);
        }
    }
}
