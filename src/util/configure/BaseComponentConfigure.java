package util.configure;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

/**
 * Base builder class providing fluent coordinate, sizing, margin, and styling
 * configuration for Swing components placed inside a {@link java.awt.GridBagLayout}.
 *
 * <p>External readers can use the fluent API to easily inspect how components are
 * positioned on the grid (x, y), their dimensions, padding (north/south/east/west insets),
 * and typography.</p>
 *
 * @param <T> The concrete configure subclass for fluent method chaining.
 */
@SuppressWarnings("unchecked")
public abstract class BaseComponentConfigure<T extends BaseComponentConfigure<T>> {
    // Grid coordinates
    protected int x = 0;
    protected int y = 0;

    // Component padding / size adjustments inside the cell
    protected int width = 0;
    protected int height = 0;

    // Cell insets (spacing around the component)
    protected int north = 0;
    protected int west = 0;
    protected int south = 0;
    protected int east = 0;

    protected boolean visible = true;
    protected Color backgroundColor;

    // Typography
    protected String font = "Century Gothic";
    protected int style = Font.PLAIN;
    protected int size = 12;

    public T setX(int x) {
        this.x = x;
        return (T) this;
    }

    public T setY(int y) {
        this.y = y;
        return (T) this;
    }

    public T setWidth(int width) {
        this.width = width;
        return (T) this;
    }

    public T setHeight(int height) {
        this.height = height;
        return (T) this;
    }

    public T setNorth(int north) {
        this.north = north;
        return (T) this;
    }

    public T setWest(int west) {
        this.west = west;
        return (T) this;
    }

    public T setSouth(int south) {
        this.south = south;
        return (T) this;
    }

    public T setEast(int east) {
        this.east = east;
        return (T) this;
    }

    public T setVisible(boolean visible) {
        this.visible = visible;
        return (T) this;
    }

    public T setInvisible() {
        this.visible = false;
        return (T) this;
    }

    public T setFont(String font) {
        this.font = font;
        return (T) this;
    }

    public T setStyle(int style) {
        this.style = style;
        return (T) this;
    }

    public T setSize(int size) {
        this.size = size;
        return (T) this;
    }

    public T setBackgroundColor(Color color) {
        this.backgroundColor = color;
        return (T) this;
    }

    public T setBackgroundColor(int red, int green, int blue) {
        int r = Math.min(255, Math.max(0, red));
        int g = Math.min(255, Math.max(0, green));
        int b = Math.min(255, Math.max(0, blue));
        this.backgroundColor = new Color(r, g, b);
        return (T) this;
    }

    protected Insets createInsets() {
        return new Insets(north, west, south, east);
    }

    protected Font createFont() {
        return new Font(font, style, size);
    }
}
