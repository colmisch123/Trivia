package util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * A custom Swing {@link JPanel} that draws a full-bleed background image behind its child components.
 *
 * <p>Uses {@link MediaResolver} to transparently resolve image paths from filesystem directories
 * or classpath resources across different runtime environments (IDE run vs compiled JAR).</p>
 */
public class JBackgroundPanel extends JPanel {
    private BufferedImage background;

    /**
     * Creates a background panel with the image loaded from the given {@link URL}.
     */
    public JBackgroundPanel(URL input) {
        if (input != null) {
            try {
                background = ImageIO.read(input);
            } catch (IOException e) {
                System.err.println("JBackgroundPanel: Could not load image from URL: " + input);
            }
        }
    }

    /**
     * Creates a background panel with the image resolved from the given path or filename.
     */
    public JBackgroundPanel(String path) {
        this(MediaResolver.resolveMediaURL(path));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            // Scale and draw image to fill the entire panel bounds
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        } else {
            // Fallback default Jeopardy blue background if image cannot be found
            g.setColor(new Color(4, 16, 84));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
