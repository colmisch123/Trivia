package menu;

import impl.JeopardyPlayer;
import intr.IBoard;
import intr.IPlayer;
import intr.ISubject;
import net.BuzzerServer;
import screen.Screen;
import util.JBackgroundPanel;
import util.JButtonListener;
import util.configure.ButtonConfigure;
import util.configure.TextConfigure;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class PlayerMenuManager extends AbstractMenu {
    private static final int MAX_PLAYERS = 15;
    private static final int MAX_PER_ROW = 5;
    private static final int MIN_PLAYERS = 1;

    private final List<JTextField> textFields = new ArrayList<>();
    private final List<JPanel> slotContainers = new ArrayList<>();
    private final List<JButton> kickButtons = new ArrayList<>();

    private final JPanel addPlayers;
    private JLabel mobileUrl;

    int amountVisible = 0;

    public PlayerMenuManager(Screen screen) {
        super(screen);

        addPlayers = new JPanel();
        addPlayers.setOpaque(false);
        addPlayers.setLayout(screen.getLayout());

        JLabel title = new JLabel();
        mobileUrl = new JLabel();

        JButtonListener back = new JButtonListener("<= Back", (e) -> {
            screen.setContentPane(screen.getStartMenu());
        });

        JButtonListener reset = new JButtonListener("Reset", (e) -> {
            this.onReset();
        });

        JButtonListener addPlayer = new JButtonListener("Add Player", (e) -> {
            if (!canAddPlayer()) {
                return;
            }
            if (amountVisible < slotContainers.size()) {
                slotContainers.get(amountVisible).setVisible(true);
                textFields.get(amountVisible).setVisible(true);
                amountVisible++;
                foreground.revalidate();
                foreground.repaint();
            }
        });

        JButtonListener removePlayer = new JButtonListener("Remove Player", (e) -> {
            if (!canRemovePlayer()) {
                return;
            }
            kickPlayerAt(amountVisible - 1);
        });

        JButtonListener confirm = new JButtonListener("Confirm Names", this::confirmPlayers);

        int north = 400;

        TextConfigure.configure()
                .setText("Enter Player Names")
                .setBackgroundColor(Screen.JEOPARDY_BLUE)
                .setX(1).setY(0).setSize(30).setStyle(Font.BOLD)
                .setWidth(100).setSouth(0)
                .confirm(title, screen, addPlayers);

        TextConfigure.configure()
                .setText("Join on phone: " + BuzzerServer.getInstance().getUrl())
                .setBackgroundColor(Screen.JEOPARDY_BLUE)
                .setTextColor(Screen.JEOPARDY_YELLOW)
                .setX(1).setY(1).setSize(16).setStyle(Font.BOLD)
                .setWidth(200).setHeight(25).setNorth(5).setSouth(5)
                .confirm(mobileUrl, screen, addPlayers);

        ButtonConfigure.configure().setX(0).setWidth(30).setHeight(10).setY(0).finish(back, screen, addPlayers);
        ButtonConfigure.configure().setX(2).setWidth(30).setHeight(10).setY(0).finish(reset, screen, addPlayers);
        ButtonConfigure.configure().setX(0).setWidth(60).setHeight(20).setY(7).setNorth(north).finish(removePlayer, screen, addPlayers);
        ButtonConfigure.configure().setX(1).setWidth(200).setHeight(30).setY(7).setNorth(north).finish(confirm, screen, addPlayers);
        ButtonConfigure.configure().setX(2).setWidth(60).setHeight(20).setY(7).setNorth(north).finish(addPlayer, screen, addPlayers);

        addPlayers.setBounds(0, 0, width, height);
        this.pane.add(addPlayers, JLayeredPane.MODAL_LAYER);

        // Populate player slots in foreground (only 1 visible initially)
        createDefaultTextFields(foreground);
    }

    @Override
    public void onSetActive() {
        BuzzerServer.getInstance().setPhase(BuzzerServer.Phase.REGISTRATION);
        if (mobileUrl != null) {
            mobileUrl.setText("Join on phone: " + BuzzerServer.getInstance().getUrl());
        }
    }

    @Override
    public void onReset() {
        BuzzerServer.getInstance().resetPlayers();
        amountVisible = 0;
        textFields.clear();
        slotContainers.clear();
        kickButtons.clear();
        foreground.removeAll();
        createDefaultTextFields(foreground);
        foreground.revalidate();
        foreground.repaint();
    }

    @Override
    public JPanel addBackground() {
        return new JBackgroundPanel("player_select1.jpg");
    }

    @Override
    public JPanel addForeground() {
        return new JPanel();
    }

    private void kickPlayerAt(int index) {
        if (index < 0 || index >= amountVisible) return;

        JTextField tf = textFields.get(index);
        String playerName = tf.getText().trim();

        // Kick from buzzer server if registered
        if (!playerName.isBlank() && !playerName.matches("^Player\\s+\\d+$")) {
            BuzzerServer.getInstance().kickPlayer(playerName);
        }

        if (amountVisible <= 1) {
            // Only 1 player visible: reset this slot to default name
            tf.setText("Player 1");
            return;
        }

        // Shift all subsequent visible player names up by 1
        for (int i = index; i < amountVisible - 1; i++) {
            textFields.get(i).setText(textFields.get(i + 1).getText());
        }

        // Clear and hide the last visible slot
        int lastIndex = amountVisible - 1;
        textFields.get(lastIndex).setText("Player " + (lastIndex + 1));
        slotContainers.get(lastIndex).setVisible(false);
        textFields.get(lastIndex).setVisible(false);
        amountVisible--;

        foreground.revalidate();
        foreground.repaint();
    }

    private void confirmPlayers(ActionEvent e) {
        IBoard<ISubject> jeopardyBoard = screen.getJeopardyBoard();
        jeopardyBoard.clearAllPlayers();
        List<IPlayer> players = jeopardyBoard.getPlayers();

        for (int i = 0; i < amountVisible; i++) {
            JTextField text = textFields.get(i);
            if (text.isVisible() && !text.getText().isBlank()) {
                players.add(new JeopardyPlayer(text.getText().trim()));
            }
        }

        if (players.isEmpty()) {
            players.add(new JeopardyPlayer("Player 1"));
        }

        String fileToOpen = screen.getFileToOpen();
        jeopardyBoard.setSubjects(fileToOpen);
        System.out.println("Successfully loaded board " + fileToOpen);

        BuzzerServer.getInstance().setPhase(BuzzerServer.Phase.BOARD);

        screen.getJeopardyBoardMenu().onReset();
        screen.getWinningMenu().onReset();

        screen.setContentPane(screen.getJeopardyBoardMenu());
    }

    @Override
    public void tick() {
        addPlayers.setBounds(0, 0, width, height);

        // Sync registered phone players into text fields
        List<String> phonePlayers = BuzzerServer.getInstance().getRegisteredPlayers();
        for (String name : phonePlayers) {
            boolean alreadyPresent = false;
            for (int i = 0; i < amountVisible; i++) {
                JTextField tf = textFields.get(i);
                if (tf.getText().trim().equalsIgnoreCase(name)) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent) {
                // First check if an existing visible slot is still a default placeholder
                boolean filledDefault = false;
                for (int i = 0; i < amountVisible; i++) {
                    JTextField tf = textFields.get(i);
                    if (tf.getText().matches("^Player\\s+\\d+$") || tf.getText().isBlank()) {
                        tf.setText(name);
                        filledDefault = true;
                        break;
                    }
                }
                // If all visible slots have non-default names, open the next slot
                if (!filledDefault && amountVisible < slotContainers.size()) {
                    JTextField tf = textFields.get(amountVisible);
                    tf.setText(name);
                    slotContainers.get(amountVisible).setVisible(true);
                    tf.setVisible(true);
                    amountVisible++;
                    foreground.revalidate();
                    foreground.repaint();
                }
            }
        }
    }

    private void createDefaultTextFields(JPanel panel) {
        amountVisible = 0;
        textFields.clear();
        slotContainers.clear();
        kickButtons.clear();
        for (int i = 0; i < MAX_PLAYERS; i++) {
            int col = i / MAX_PER_ROW;
            int r = i % MAX_PER_ROW;
            playerSlotCreator(i, col, r, panel);
        }
    }

    private void playerSlotCreator(int index, int col, int r, JPanel panel) {
        JPanel slot = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        slot.setOpaque(false);

        JTextField enterPlayer = new JTextField();
        enterPlayer.setText("Player " + (index + 1));
        enterPlayer.setColumns(16);
        enterPlayer.setHorizontalAlignment(SwingConstants.CENTER);
        enterPlayer.setFont(new Font("Century Gothic", Font.PLAIN, 15));
        enterPlayer.setPreferredSize(new Dimension(170, 30));
        enterPlayer.setBackground(Color.WHITE);
        enterPlayer.setForeground(Color.BLACK);

        JButton kickBtn = new JButton("X");
        kickBtn.setFont(new Font("Century Gothic", Font.BOLD, 16));
        kickBtn.setFocusPainted(false);
        kickBtn.setContentAreaFilled(false);
        kickBtn.setOpaque(true);
        kickBtn.setPreferredSize(new Dimension(38, 30));
        kickBtn.setMargin(new Insets(0, 0, 0, 0));
        kickBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        kickBtn.setToolTipText("Kick this player");

        Color normalBg = new Color(235, 235, 235);
        Color normalFg = new Color(180, 40, 40);
        Color hoverBg = new Color(220, 30, 30);
        Color hoverFg = Color.WHITE;

        kickBtn.setBackground(normalBg);
        kickBtn.setForeground(normalFg);
        kickBtn.setBorder(BorderFactory.createLineBorder(new Color(200, 70, 70), 1));

        kickBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                kickBtn.setBackground(hoverBg);
                kickBtn.setForeground(hoverFg);
                kickBtn.setBorder(BorderFactory.createLineBorder(new Color(180, 20, 20), 1));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                kickBtn.setBackground(normalBg);
                kickBtn.setForeground(normalFg);
                kickBtn.setBorder(BorderFactory.createLineBorder(new Color(200, 70, 70), 1));
            }
        });

        final int slotIndex = index;
        kickBtn.addActionListener(e -> kickPlayerAt(slotIndex));

        slot.add(enterPlayer);
        slot.add(kickBtn);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = col;
        gbc.gridy = r;
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.anchor = GridBagConstraints.CENTER;
        screen.getLayout().setConstraints(slot, gbc);
        panel.add(slot);

        textFields.add(enterPlayer);
        slotContainers.add(slot);
        kickButtons.add(kickBtn);

        // Only player 1 starts visible
        if (index == 0) {
            slot.setVisible(true);
            enterPlayer.setVisible(true);
            amountVisible = 1;
        } else {
            slot.setVisible(false);
            enterPlayer.setVisible(false);
        }
    }

    private boolean canAddPlayer() {
        return amountVisible < MAX_PLAYERS;
    }

    private boolean canRemovePlayer() {
        return amountVisible > MIN_PLAYERS;
    }
}
