package menu;

import intr.IBoard;
import intr.IPlayer;
import intr.IQuestion;
import intr.ISubject;
import net.BuzzerServer;
import screen.Screen;
import util.JBackgroundPanel;
import util.JButtonListener;
import util.configure.ButtonConfigure;
import util.configure.TextConfigure;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RewardMenuManager extends AbstractMenu implements Consumer<IQuestion> {
    private IQuestion currentQuestion;
    private IPlayer selectedPlayer = null;
    private JButton selectedButton = null;

    private JButtonListener nextButton;
    private JButtonListener customPointsButton;
    private final List<JButtonListener> playerButtons = new ArrayList<>();

    public RewardMenuManager(Screen screen) {
        super(screen);
    }

    @Override
    public void onReset() {
        selectedPlayer = null;
        selectedButton = null;
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
    public void onSetActive() {
        foreground.removeAll();
        selectedPlayer = null;
        selectedButton = null;
        playerButtons.clear();

        IBoard<ISubject> board = screen.getJeopardyBoard();
        List<IPlayer> players = board.getPlayers();

        int questionValue = currentQuestion != null ? currentQuestion.getValue() : 0;
        boolean isForbiddenTile = questionValue <= -500000 || 
                (currentQuestion != null && "DO NOT CHOOSE THIS TILE".equalsIgnoreCase(currentQuestion.getTileText()));

        // Row 0: Title
        JLabel title = new JLabel();
        TextConfigure.configure()
                .setX(1).setY(0)
                .setText("Who Answered Correctly?")
                .setSize(24).setStyle(Font.BOLD)
                .setTextColor(Screen.JEOPARDY_YELLOW)
                .setBackgroundColor(Screen.JEOPARDY_BLUE)
                .setWidth(200).setHeight(30)
                .setNorth(20).setSouth(5)
                .confirm(title, screen, foreground);

        // Row 1: Subtitle / Value Info
        JLabel subtitle = new JLabel();
        String initialSubtitle = isForbiddenTile
                ? "FORBIDDEN TILE! Value: -$1,000,000  |  Select player to penalize, or click Next to skip"
                : "Question Value: $" + questionValue + "  |  Click a player to select (click again to deselect)";
        TextConfigure.configure()
                .setX(1).setY(1)
                .setText(initialSubtitle)
                .setSize(14).setStyle(Font.BOLD)
                .setTextColor(isForbiddenTile ? new Color(255, 100, 100) : Color.WHITE)
                .setBackgroundColor(Screen.JEOPARDY_BLUE)
                .setWidth(200).setHeight(25)
                .setNorth(5).setSouth(15)
                .confirm(subtitle, screen, foreground);

        // Row 2, Col 0: Custom Points Adjustment
        customPointsButton = new JButtonListener("+/- Custom Pts", (e) -> {
            if (selectedPlayer == null) {
                JOptionPane.showMessageDialog(
                        screen,
                        "Please select a player first to adjust their score.",
                        "No Player Selected",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            String input = JOptionPane.showInputDialog(
                    screen,
                    "Enter points to add/subtract for " + selectedPlayer.getNames() + "\n(e.g. 500 for bonus, or -200 for penalty):",
                    "Manual Score Adjustment",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (input != null && !input.isBlank()) {
                try {
                    int customVal = Integer.parseInt(input.trim().replace(",", ""));
                    selectedPlayer.addScore(customVal);
                    if (selectedButton != null) {
                        selectedButton.setText(selectedPlayer.getNames() + " ($" + selectedPlayer.getScore() + ")");
                    }
                    JOptionPane.showMessageDialog(
                            screen,
                            "Updated " + selectedPlayer.getNames() + "'s score to $" + selectedPlayer.getScore(),
                            "Score Adjusted",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(screen, "Invalid number entered.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        customPointsButton.setFont(new Font("Century Gothic", Font.BOLD, 12));
        customPointsButton.setBackground(Color.WHITE);
        customPointsButton.setForeground(Color.BLACK);
        ButtonConfigure.configure()
                .setX(0).setY(2)
                .setWidth(140).setHeight(30)
                .setNorth(5).setSouth(15).setWest(10).setEast(10)
                .finish(customPointsButton, screen, foreground);

        // Row 2, Col 2: Next Button
        nextButton = new JButtonListener("Next =>", (e) -> {
            if (selectedPlayer != null && currentQuestion != null) {
                selectedPlayer.addScore(currentQuestion.getValue());
            }
            selectedPlayer = null;
            selectedButton = null;

            if (screen.getJeopardyBoardMenu() instanceof BoardMenuManager menu) {
                if (menu.isBoardEmpty()) {
                    screen.setContentPane(screen.getWinningMenu());
                    return;
                }
            }
            screen.setContentPane(screen.getJeopardyBoardMenu());
        });
        nextButton.setBackground(Color.WHITE);
        nextButton.setForeground(Color.BLACK);
        ButtonConfigure.configure()
                .setX(2).setY(2)
                .setWidth(130).setHeight(30)
                .setNorth(5).setSouth(15).setWest(10).setEast(10)
                .finish(nextButton, screen, foreground);

        // Rows 3+: Player Selection Buttons (Grid: 3 columns wide: col 0, 1, 2)
        int cols = 3;
        for (int i = 0; i < players.size(); i++) {
            IPlayer player = players.get(i);
            int col = i % cols;
            int row = 3 + (i / cols);

            JButtonListener playerBtn = new JButtonListener(player.getNames() + " ($" + player.getScore() + ")", null);
            playerBtn.setBackground(Color.WHITE);
            playerBtn.setForeground(Color.BLACK);

            playerBtn.addActionListener((e) -> {
                if (selectedPlayer == player) {
                    // Deselect if already selected
                    selectedPlayer = null;
                    if (selectedButton != null) {
                        selectedButton.setBackground(Color.WHITE);
                        selectedButton.setForeground(Color.BLACK);
                    }
                    selectedButton = null;
                } else {
                    // Unhighlight previous selection
                    if (selectedButton != null) {
                        selectedButton.setBackground(Color.WHITE);
                        selectedButton.setForeground(Color.BLACK);
                    }
                    selectedPlayer = player;
                    selectedButton = playerBtn;
                    selectedButton.setBackground(Screen.JEOPARDY_YELLOW);
                    selectedButton.setForeground(Color.BLACK);
                }
            });

            playerButtons.add(playerBtn);
            ButtonConfigure.configure()
                    .setX(col).setY(row)
                    .setWidth(120).setHeight(30)
                    .setNorth(6).setSouth(6).setWest(10).setEast(10)
                    .finish(playerBtn, screen, foreground);
        }

        // Auto-select buzzer winner if someone buzzed in and was not marked wrong
        String buzzerWinner = BuzzerServer.getInstance().getLastSuccessfulWinner();
        if (buzzerWinner != null) {
            for (int i = 0; i < players.size(); i++) {
                IPlayer p = players.get(i);
                if (p.getNames().trim().equalsIgnoreCase(buzzerWinner.trim())) {
                    selectedPlayer = p;
                    if (i < playerButtons.size()) {
                        selectedButton = playerButtons.get(i);
                        selectedButton.setBackground(Screen.JEOPARDY_YELLOW);
                        selectedButton.setForeground(Color.BLACK);
                    }
                    break;
                }
            }
        }

        foreground.setOpaque(false);
        foreground.setLayout(screen.getLayout());
        foreground.setBounds(0, 0, width, height);
    }

    @Override
    public void onRemove() {
        foreground.removeAll();
    }

    @Override
    public void tick() {
    }

    @Override
    public void accept(IQuestion iQuestion) {
        this.currentQuestion = iQuestion;
    }
}
