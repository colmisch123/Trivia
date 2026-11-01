package menu;

import intr.IBoard;
import intr.IPlayer;
import intr.IQuestion;
import intr.ISubject;
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
    private boolean isDeductMode = false;

    private JButtonListener editButton;
    private JButtonListener nextButton;
    private final List<JButtonListener> playerButtons = new ArrayList<>();

    public RewardMenuManager(Screen screen) {
        super(screen);
    }

    @Override
    public void onReset() {
        selectedPlayer = null;
        selectedButton = null;
        isDeductMode = false;
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
        isDeductMode = false;
        playerButtons.clear();

        IBoard<ISubject> board = screen.getJeopardyBoard();
        List<IPlayer> players = board.getPlayers();

        int questionValue = currentQuestion != null ? currentQuestion.getValue() : 0;

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
        TextConfigure.configure()
                .setX(1).setY(1)
                .setText("Question Value: $" + questionValue + "  |  Click a player to select (click again to deselect)")
                .setSize(14).setStyle(Font.BOLD)
                .setTextColor(Color.WHITE)
                .setBackgroundColor(Screen.JEOPARDY_BLUE)
                .setWidth(200).setHeight(25)
                .setNorth(5).setSouth(15)
                .confirm(subtitle, screen, foreground);

        // Row 2, Col 0: Mode Toggle (Award vs Deduct)
        editButton = new JButtonListener("Mode: Award (+)", (e) -> {
            isDeductMode = !isDeductMode;
            if (isDeductMode) {
                editButton.setText("Mode: Deduct (-)");
                editButton.setBackground(new Color(255, 80, 80));
                editButton.setForeground(Color.WHITE);
                if (selectedButton != null) {
                    selectedButton.setBackground(new Color(255, 80, 80));
                    selectedButton.setForeground(Color.WHITE);
                }
            } else {
                editButton.setText("Mode: Award (+)");
                editButton.setBackground(Color.WHITE);
                editButton.setForeground(Color.BLACK);
                if (selectedButton != null) {
                    selectedButton.setBackground(Screen.JEOPARDY_YELLOW);
                    selectedButton.setForeground(Color.BLACK);
                }
            }
        });
        editButton.setBackground(Color.WHITE);
        editButton.setForeground(Color.BLACK);
        ButtonConfigure.configure()
                .setX(0).setY(2)
                .setWidth(130).setHeight(30)
                .setNorth(5).setSouth(15).setWest(10).setEast(10)
                .finish(editButton, screen, foreground);

        // Row 2, Col 2: Next Button (Awards/deducts points once upon advancing)
        nextButton = new JButtonListener("Next =>", (e) -> {
            if (selectedPlayer != null && currentQuestion != null) {
                int value = currentQuestion.getValue();
                if (isDeductMode) {
                    selectedPlayer.addScore(-value);
                } else {
                    selectedPlayer.addScore(value);
                }
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
                    if (isDeductMode) {
                        selectedButton.setBackground(new Color(255, 80, 80));
                        selectedButton.setForeground(Color.WHITE);
                    } else {
                        selectedButton.setBackground(Screen.JEOPARDY_YELLOW);
                        selectedButton.setForeground(Color.BLACK);
                    }
                }
            });

            playerButtons.add(playerBtn);
            ButtonConfigure.configure()
                    .setX(col).setY(row)
                    .setWidth(120).setHeight(30)
                    .setNorth(6).setSouth(6).setWest(10).setEast(10)
                    .finish(playerBtn, screen, foreground);
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
        // No auto-clearing timer
    }

    @Override
    public void accept(IQuestion iQuestion) {
        this.currentQuestion = iQuestion;
    }
}
