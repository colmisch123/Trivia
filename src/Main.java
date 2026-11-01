import screen.Screen;

/**
 * Application entry point for Jeopardy 2.0.
 *
 * <p>Initializes the main game window ({@link Screen}) and starts the 60 FPS
 * game loop thread.</p>
 */
public class Main {
    public static void main(String[] args) {
        Screen frame = new Screen();
        frame.setVisible(true);

        Thread thread = new Thread(frame, "Jeopardy-MainLoop");
        thread.start();
    }
}
