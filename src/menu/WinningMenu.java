package menu;

import intr.IPlayer;
import screen.Screen;
import util.JBackgroundPanel;
import util.JButtonListener;
import util.configure.ButtonConfigure;
import util.configure.TextConfigure;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * End-Game Podium and Final Placement screen.
 *
 * <p>Displays final contestant scores sorted in descending order when all board questions
 * have been answered, and provides a button to return to the Main Menu.</p>
 */
public class WinningMenu extends AbstractMenu {
    private boolean hasInit = false;

    public WinningMenu(Screen screen) {
        super(screen);
    }

    @Override
    public void onSetActive() {
        if (hasInit) {
            return;
        }
        hasInit = true;
        foreground.removeAll();

        screen.syncLatePlayers();

        // Sort players by score descending
        List<IPlayer> players = new ArrayList<>(screen.getJeopardyBoard().getPlayers());
        players.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        JLabel placements = new JLabel();
        TextConfigure.configure()
                .setBackgroundColor(Screen.JEOPARDY_BLUE)
                .setText("Final Placements")
                .setSize(40).setStyle(Font.BOLD)
                .setNorth(30).setSouth(20)
                .confirm(placements, screen, foreground);

        // Display top players
        int max = Math.min(players.size(), 10);
        for (int i = 0; i < max; i++) {
            IPlayer p = players.get(i);
            JLabel label = new JLabel();
            String placeText = (i + 1) + ". " + p.getNames() + " - $" + p.getScore();
            TextConfigure.configure()
                    .setBackgroundColor(Screen.JEOPARDY_BLUE)
                    .setText(placeText)
                    .setY(i + 1)
                    .setSize(22).setStyle(Font.BOLD)
                    .setNorth(5).setSouth(5)
                    .confirm(label, screen, foreground);
        }

        JButtonListener mainMenuBtn = new JButtonListener("Main Menu", (e) -> {
            screen.setContentPane(screen.getStartMenu());
        });
        ButtonConfigure.configure()
                .setY(max + 1)
                .setWidth(100).setHeight(30)
                .setNorth(30)
                .finish(mainMenuBtn, screen, foreground);

        foreground.setOpaque(false);
        foreground.setLayout(screen.getLayout());
        foreground.setBounds(0, 0, width, height);
    }

    @Override
    public void onReset() {
        hasInit = false;
        foreground.removeAll();
    }

    @Override
    public JPanel addBackground() {
        return new JBackgroundPanel("reward_menu.jpg");
    }

    @Override
    public JPanel addForeground() {
        return new JPanel();
    }

    @Override
    public void tick() {
    }
}
