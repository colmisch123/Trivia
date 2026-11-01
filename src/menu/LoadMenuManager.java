package menu;

import screen.Screen;
import util.JBackgroundPanel;
import util.JButtonListener;
import util.JsonHandler;
import util.configure.ButtonConfigure;
import util.configure.TextConfigure;
import util.configure.TextFieldConfigure;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Board Selector and Loader menu.
 *
 * <p>Allows the host to choose which question board to play before starting a game:
 * <ul>
 *   <li>Lists all available {@code .json} files found in {@code SavedBoards/}</li>
 *   <li>Validates the selected file using {@link JsonHandler#validateBoardFile(String)}</li>
 *   <li>Shows informative error dialogs if the board file is missing or malformed</li>
 *   <li>Sets {@link Screen#setFileToOpen(String)} upon confirmation</li>
 * </ul>
 * </p>
 */
public class LoadMenuManager extends AbstractMenu {
    private JTextField enterSubject;
    private JLabel currentLoadedFile;
    private JLabel availableBoardsLabel;
    private JLabel statusLabel;

    public LoadMenuManager(Screen screen) {
        super(screen);
    }

    @Override
    public void onReset() {
        if (enterSubject != null) {
            enterSubject.setText(screen.getFileToOpen());
        }
        if (statusLabel != null) {
            statusLabel.setText("");
        }
    }

    @Override
    public JPanel addBackground() {
        return new JBackgroundPanel("load.jpg");
    }

    @Override
    public JPanel addForeground() {
        JPanel panel = new JPanel();

        JLabel title = new JLabel();
        TextConfigure.configure()
                .setText("Enter Jeopardy Board File:")
                .setSize(30).setStyle(Font.BOLD)
                .setWidth(100).setNorth(20).setSouth(30)
                .confirm(title, screen, panel);

        enterSubject = new JTextField();
        TextFieldConfigure.configure()
                .setText(screen.getFileToOpen()).setSize(15).setY(1)
                .setHeight(25).setWidth(400).setStyle(Font.BOLD)
                .setNorth(10).setEast(10).setSouth(10).setWest(10)
                .confirm(enterSubject, screen, panel);

        JButtonListener confirm = new JButtonListener("Confirm", (e) -> {
            String input = enterSubject.getText();
            JsonHandler.BoardValidationResult result = JsonHandler.validateBoardFile(input);

            if (!result.isValid()) {
                statusLabel.setText(result.getMessage());
                statusLabel.setForeground(new Color(255, 80, 80));
                statusLabel.setVisible(true);
                JOptionPane.showMessageDialog(
                        screen,
                        result.getMessage(),
                        "Error Loading Board",
                        JOptionPane.ERROR_MESSAGE
                );
            } else {
                screen.setFileToOpen(result.getResolvedFileName());
                enterSubject.setText(result.getResolvedFileName());
                currentLoadedFile.setText(result.getResolvedFileName() + " Currently Loaded");
                statusLabel.setText(result.getMessage());
                statusLabel.setForeground(new Color(80, 255, 80));
                statusLabel.setVisible(true);
                JOptionPane.showMessageDialog(
                        screen,
                        "Successfully loaded: " + result.getResolvedFileName(),
                        "Board Loaded",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        ButtonConfigure.configure()
                .setX(1).setY(1).setWidth(60).setHeight(25).setText("Confirm").setWest(10)
                .finish(confirm, screen, panel);

        statusLabel = new JLabel();
        TextConfigure.configure()
                .setText("").setY(2).setStyle(Font.BOLD).setSize(14).setWidth(100).setNorth(10)
                .confirm(statusLabel, screen, panel);

        availableBoardsLabel = new JLabel();
        TextConfigure.configure()
                .setText(getAvailableBoardsText()).setY(3).setStyle(Font.PLAIN).setSize(14).setWidth(100).setNorth(10)
                .confirm(availableBoardsLabel, screen, panel);

        currentLoadedFile = new JLabel();
        TextConfigure.configure()
                .setText(screen.getFileToOpen() + " Currently Loaded").setY(4).setStyle(Font.BOLD).setSize(16).setWidth(100).setNorth(20)
                .confirm(currentLoadedFile, screen, panel);

        JButtonListener exit = new JButtonListener("<= Back", (e) -> {
            screen.setContentPane(screen.getStartMenu());
        });

        ButtonConfigure.configure()
                .setX(0).setY(5).setWidth(60).setHeight(25).setText("<= Back").setNorth(20)
                .finish(exit, screen, panel);

        return panel;
    }

    private String getAvailableBoardsText() {
        List<String> files = JsonHandler.getAvailableBoardFiles();
        if (files.isEmpty()) {
            return "SavedBoards folder is currently empty.";
        }
        return "Available Boards: " + String.join(", ", files);
    }

    @Override
    public void onSetActive() {
        if (enterSubject != null) {
            enterSubject.setText(screen.getFileToOpen());
        }
        if (currentLoadedFile != null) {
            currentLoadedFile.setText(screen.getFileToOpen() + " Currently Loaded");
        }
        if (availableBoardsLabel != null) {
            availableBoardsLabel.setText(getAvailableBoardsText());
        }
        if (statusLabel != null) {
            statusLabel.setText("");
        }
    }

    @Override
    public void tick() {
    }
}
