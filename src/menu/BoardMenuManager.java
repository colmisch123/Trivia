package menu;

import intr.IPlayer;
import intr.IQuestion;
import intr.ISubject;
import screen.Screen;
import util.JBackgroundPanel;
import util.configure.ButtonConfigure;
import util.configure.TextConfigure;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Jeopardy Game Board screen.
 *
 * <p>Major functionalities:
 * <ul>
 *   <li>Dynamically generates the trivia board grid based on categories (subjects) in {@code Screen.jeopardyBoard}</li>
 *   <li>Displays category header titles across the top</li>
 *   <li>Renders point value tiles (e.g. "$100", "$200") or custom tile text (e.g. "DO NOT CHOOSE THIS TILE")</li>
 *   <li>Clicking an active tile marks it played (hides the button) and opens {@link QuestionMenuManager}</li>
 *   <li>Displays real-time scores for all contestants across the bottom</li>
 *   <li>Tracks remaining board tiles to trigger the final podium screen when cleared</li>
 * </ul>
 * </p>
 */
public class BoardMenuManager extends AbstractMenu {
    private boolean hasInit = false;
    private List<JLabel> playerLabels;
    private List<JButton> allBoardButtons;
    private int totalBoardButtons = 0;

    public BoardMenuManager(Screen screen) {
        super(screen);
    }

    @Override
    public JPanel addBackground() {
        return new JBackgroundPanel("board.jpg");
    }

    @Override
    public JPanel addForeground() {
        return new JPanel();
    }

    /**
     * Updates player score labels at 60 FPS. Dynamically refreshes badges if players join late.
     */
    @Override
    public void tick() {
        List<IPlayer> players = screen.getJeopardyBoard().getPlayers();
        if (players == null) {
            return;
        }

        if (playerLabels == null || players.size() != playerLabels.size()) {
            SwingUtilities.invokeLater(this::refreshPlayerLabels);
            return;
        }

        for (int i = 0; i < Math.min(players.size(), playerLabels.size()); i++) {
            JLabel l = playerLabels.get(i);
            IPlayer p = players.get(i);
            l.setText(p.getNames() + ": $" + p.getScore());
        }
    }

    @Override
    public void onSetActive() {
        screen.syncLatePlayers();
        if (hasInit) {
            refreshPlayerLabels();
            return;
        }

        allBoardButtons = new ArrayList<>();
        totalBoardButtons = 0;

        List<ISubject> subjects = screen.getJeopardyBoard().getSubjects();

        Color blue = Screen.JEOPARDY_BLUE;
        Color yellow = Screen.JEOPARDY_YELLOW;

        // 1. Generate category columns and question tiles
        for (int i = 0; i < subjects.size(); i++) {
            ISubject s = subjects.get(i);

            // Category title header
            JLabel subjectTitle = new JLabel();
            TextConfigure.configure()
                    .setX(i).setY(0)
                    .setText(s.getTitle())
                    .setTextColor(yellow)
                    .setStyle(Font.BOLD).setSize(16)
                    .setWidth(75).setHeight(25).setSouth(25)
                    .setBackgroundColor(blue)
                    .confirm(subjectTitle, screen, foreground);

            // Question tiles under this category
            List<IQuestion> questions = s.getQuestions();
            for (int j = 0; j < questions.size(); j++) {
                IQuestion q = questions.get(j);
                JButton button = new JButton();

                ActionListener action = (e) -> {
                    totalBoardButtons--;
                    button.setVisible(false);

                    AbstractMenu menu = screen.getQuestionMenu();
                    if (menu instanceof QuestionMenuManager qMenu) {
                        qMenu.accept(q);
                    }
                    screen.setContentPane(menu);
                };

                button.addActionListener(action);
                allBoardButtons.add(button);
                totalBoardButtons++;

                // Format button label: custom text or standard dollar amount
                String tileText = q.getTileText();
                String buttonText;
                if (tileText != null && !tileText.isBlank()) {
                    buttonText = "<html><center>" + tileText.replace(" ", "<br>") + "</center></html>";
                } else {
                    buttonText = "$" + q.getValue();
                }

                ButtonConfigure.configure()
                        .setX(i).setY(j + 1)
                        .setText(buttonText)
                        .setWidth(90).setHeight(65).setSouth(5)
                        .setBackgroundColor(blue)
                        .setForegroundColor(yellow)
                        .finish(button, screen, foreground);

                if (tileText != null && !tileText.isBlank()) {
                    button.setFont(new Font("Century Gothic", Font.BOLD, 10));
                } else {
                    button.setFont(new Font("Century Gothic", Font.BOLD, 16));
                }
            }
        }

        // 2. Generate bottom contestant score badges
        refreshPlayerLabels();

        foreground.setOpaque(false);
        foreground.setLayout(screen.getLayout());
        foreground.setBounds(0, 0, width, height);

        hasInit = true;
    }

    /**
     * Dynamically rebuilds contestant score badges at the bottom of the board
     * whenever new players join or scores change.
     */
    private void refreshPlayerLabels() {
        if (playerLabels != null) {
            for (JLabel lbl : playerLabels) {
                foreground.remove(lbl);
            }
        }
        playerLabels = new ArrayList<>();
        List<ISubject> subjects = screen.getJeopardyBoard().getSubjects();
        int maxPlayersForColumn = Math.max(1, subjects != null ? subjects.size() : 1);
        List<IPlayer> players = screen.getJeopardyBoard().getPlayers();
        Color blue = Screen.JEOPARDY_BLUE;
        Color yellow = Screen.JEOPARDY_YELLOW;

        int x = 0;
        int yOffset = 1;

        for (int i = 0; i < players.size(); i++) {
            IPlayer player = players.get(i);
            int baseQuestions = (subjects != null && i % maxPlayersForColumn < subjects.size())
                    ? subjects.get(i % maxPlayersForColumn).getQuestions().size()
                    : 5;
            int y = baseQuestions + yOffset;
            JLabel label = new JLabel();

            TextConfigure.configure()
                    .setX(x).setY(y)
                    .setText(player.getNames() + ": $" + player.getScore())
                    .setTextColor(yellow).setStyle(Font.BOLD).setSize(16)
                    .setWidth(75).setHeight(25)
                    .setBackgroundColor(blue)
                    .confirm(label, screen, foreground);

            playerLabels.add(label);

            x++;
            if (x >= maxPlayersForColumn) {
                x = 0;
                yOffset++;
            }
        }
        foreground.revalidate();
        foreground.repaint();
    }

    @Override
    public void onReset() {
        hasInit = false;
        totalBoardButtons = 0;
        foreground.removeAll();
    }

    /**
     * Checks if all questions on the board have been played.
     */
    public boolean isBoardEmpty() {
        return totalBoardButtons <= 0;
    }
}
