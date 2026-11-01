package util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class JBackgroundPanel extends JPanel {
    private BufferedImage background;

    public JBackgroundPanel(URL input) {
        if (input != null) {
            try {
                background = ImageIO.read(input);
            } catch (IOException e) {
                System.err.println("Could not load image from URL: " + input);
                e.printStackTrace();
            }
        }
    }

    public JBackgroundPanel(String url) {
        this(resolveResource(url));
    }

    private static URL resolveResource(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        String clean = path.startsWith("/") ? path.substring(1) : path;

        // Try various classpath locations
        String[] candidates = {
            "/" + clean,
            "/Resource/" + clean,
            clean,
            "Resource/" + clean
        };

        for (String candidate : candidates) {
            URL res = JBackgroundPanel.class.getResource(candidate);
            if (res != null) {
                return res;
            }
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            for (String candidate : candidates) {
                String clCandidate = candidate.startsWith("/") ? candidate.substring(1) : candidate;
                URL res = cl.getResource(clCandidate);
                if (res != null) {
                    return res;
                }
            }
        }

        // Fallback to relative file paths on disk (e.g. running from workspace in IDEs)
        String[] filePaths = {
            "src/Resource/" + clean,
            "Resource/" + clean,
            clean
        };

        for (String fp : filePaths) {
            File f = new File(fp);
            if (f.exists() && f.isFile()) {
                try {
                    return f.toURI().toURL();
                } catch (Exception ignored) {
                }
            }
        }

        System.err.println("Warning: Unable to find background image resource for: " + path);
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        } else {
            g.setColor(new Color(4, 16, 84));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
