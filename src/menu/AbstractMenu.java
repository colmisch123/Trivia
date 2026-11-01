package menu;

import intr.IBackgroundForeground;
import intr.ITickable;
import screen.Screen;

import javax.swing.*;
import java.awt.*;

/**
 * Abstract base class for all game screen menus.
 *
 * <p>Each menu consists of a {@link JLayeredPane} with:
 * <ul>
 *   <li><b>DEFAULT_LAYER</b>: Full-screen background image (via {@link #addBackground()})</li>
 *   <li><b>PALETTE_LAYER</b>: Foreground content panel (via {@link #addForeground()})</li>
 * </ul>
 * Also integrates lifecycle callbacks:
 * <ul>
 *   <li>{@link #onSetActive()}: Executed when the screen becomes active in the window</li>
 *   <li>{@link #onRemove()}: Executed when navigating away to another screen</li>
 *   <li>{@link #onReset()}: Restores initial default state</li>
 *   <li>{@link #tick()}: Invoked on each frame of the 60 FPS game loop</li>
 * </ul>
 * </p>
 */
public abstract class AbstractMenu implements IBackgroundForeground, ITickable {
    protected JLayeredPane pane;
    protected JPanel background;
    protected JPanel foreground;
    protected GridBagLayout layout;
    protected Screen screen;

    protected int width = 0;
    protected int height = 0;

    public AbstractMenu(Screen screen) {
        this.screen = screen;

        priorityInit();

        this.pane = new JLayeredPane();
        this.layout = screen.getLayout();
        this.width = screen.getWidth();
        this.height = screen.getHeight();
        this.background = addBackground();
        this.foreground = addForeground();

        enableLayout();

        screen.getAllMenus().add(this);

        pane.setBounds(0, 0, width, height);
        background.setBounds(0, 0, width, height);
        foreground.setBounds(0, 0, width, height);

        foreground.setOpaque(false);

        this.pane.add(background, JLayeredPane.DEFAULT_LAYER);
        this.pane.add(foreground, JLayeredPane.PALETTE_LAYER);
    }

    protected void enableLayout() {
        this.foreground.setLayout(screen.getLayout());
    }

    /**
     * Hook for subclasses requiring initialization before pane setup.
     */
    protected void priorityInit() {}

    public JLayeredPane getPane() {
        return this.pane;
    }

    /**
     * Called by the main loop to synchronize dimensions and trigger frame tick logic.
     */
    public void update() {
        this.width = screen.getWidth();
        this.height = screen.getHeight();

        this.foreground.setBounds(0, 0, width, height);
        this.background.setBounds(0, 0, width, height);
        this.pane.setBounds(0, 0, width, height);
        tick();
    }

    /**
     * Invoked when this menu becomes the active content pane.
     */
    public void onSetActive() {}

    /**
     * Invoked when this menu is being replaced by another screen.
     */
    public void onRemove() {}

    /**
     * Resets this menu to its initial state.
     */
    public abstract void onReset();
}
