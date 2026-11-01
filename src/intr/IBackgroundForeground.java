package intr;

import javax.swing.JPanel;

/**
 * Interface contract defining the background and foreground panels of a layered screen menu.
 */
public interface IBackgroundForeground {
    JPanel addBackground();
    JPanel addForeground();
}
