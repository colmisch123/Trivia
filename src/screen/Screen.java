package screen;

import impl.JeopardyBoard;
import impl.JeopardyPlayer;
import intr.IBoard;
import intr.IPlayer;
import intr.ISubject;
import menu.*;
import net.BuzzerServer;
import util.JsonHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application window and game coordinator.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Hosts the primary {@link JFrame} (960x720)</li>
 *   <li>Instantiates and manages screen transitions across all game menus</li>
 *   <li>Runs the 60 FPS game loop thread ({@link #run()}) to tick active menus</li>
 *   <li>Starts and cleanly shuts down the embedded {@link BuzzerServer} for mobile phone support</li>
 *   <li>Stores current board and player state in {@link #jeopardyBoard}</li>
 * </ul>
 * </p>
 */
public class Screen extends JFrame implements Runnable {
    public static final Color JEOPARDY_BLUE = new Color(4, 16, 84);
    public static final Color JEOPARDY_YELLOW = new Color(255, 255, 0);
    public static final String DEFAULT_FILE_TO_OPEN = "Halloween_Trivia.json";
    public static final int FPS = 60;

    private String fileToOpen = DEFAULT_FILE_TO_OPEN;
    private int fpsCount = 0;

    private AbstractMenu currentMenu;
    private final AbstractMenu startMenu;
    private final AbstractMenu jeopardyBoardMenu;
    private final AbstractMenu questionMenu;
    private final AbstractMenu playerMenu;
    private final AbstractMenu rewardMenu;
    private final AbstractMenu loadMenu;
    private final AbstractMenu questionCreator;
    private final AbstractMenu winningMenu;

    private final IBoard<ISubject> jeopardyBoard;
    private final List<AbstractMenu> allMenus = new ArrayList<>();
    private final GridBagLayout layout = new GridBagLayout();

    public Screen() {
        setTitle("JEOPARDY");
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        setBounds(screenSize.width / 4, screenSize.height / 10, 960, 720);
        setLayout(null);

        jeopardyBoard = new JeopardyBoard();

        // Launch mobile phone buzzer server
        BuzzerServer.getInstance().start();
        BuzzerServer.setOnPlayerRegisteredCallback(name -> {
            SwingUtilities.invokeLater(() -> syncLatePlayer(name));
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                BuzzerServer.getInstance().stop();
            }
        });

        // Initialize all application menus
        startMenu = new StartMenuManager(this);
        jeopardyBoardMenu = new BoardMenuManager(this);
        questionMenu = new QuestionMenuManager(this);
        playerMenu = new PlayerMenuManager(this);
        rewardMenu = new RewardMenuManager(this);
        loadMenu = new LoadMenuManager(this);
        questionCreator = new QuestionsCreatorMenu(this);
        winningMenu = new WinningMenu(this);

        setContentPane(startMenu);
    }

    /**
     * Transitions the displayed screen to the specified menu, notifying both menus
     * and synchronizing the mobile buzzer server phase.
     */
    public void setContentPane(AbstractMenu menu) {
        if (currentMenu != null) {
            currentMenu.onRemove();
        }
        menu.onSetActive();
        currentMenu = menu;
        setContentPane(menu.getPane());

        // Sync mobile server phase with current desktop view
        if (menu == playerMenu) {
            BuzzerServer.getInstance().setPhase(BuzzerServer.Phase.REGISTRATION);
        } else if (menu == jeopardyBoardMenu) {
            BuzzerServer.getInstance().setPhase(BuzzerServer.Phase.BOARD);
        } else if (menu == rewardMenu) {
            BuzzerServer.getInstance().setPhase(BuzzerServer.Phase.REWARD);
        }
    }

    public GridBagLayout getLayout() {
        return this.layout;
    }

    public List<AbstractMenu> getAllMenus() {
        return allMenus;
    }

    public AbstractMenu getStartMenu() {
        return startMenu;
    }

    public AbstractMenu getJeopardyBoardMenu() {
        return jeopardyBoardMenu;
    }

    public AbstractMenu getQuestionMenu() {
        return questionMenu;
    }

    public AbstractMenu getPlayerMenu() {
        return playerMenu;
    }

    public AbstractMenu getRewardMenu() {
        return rewardMenu;
    }

    public AbstractMenu getLoadMenu() {
        return loadMenu;
    }

    public AbstractMenu getQuestionCreator() {
        return questionCreator;
    }

    public AbstractMenu getWinningMenu() {
        return winningMenu;
    }

    public IBoard<ISubject> getJeopardyBoard() {
        return jeopardyBoard;
    }

    public String getFileToOpen() {
        return fileToOpen;
    }

    public void setFileToOpen(String fileToOpen) {
        this.fileToOpen = JsonHandler.normalizeFileName(fileToOpen);
    }

    /**
     * Synchronizes any contestants registered on mobile phones into the active game board.
     * Preserves existing player profiles and scores for contestants who already joined.
     */
    public synchronized void syncLatePlayers() {
        List<String> registered = BuzzerServer.getInstance().getRegisteredPlayers();
        for (String name : registered) {
            syncLatePlayer(name);
        }
    }

    /**
     * Ensures a contestant is represented in the active game board.
     * If they already have an active profile in jeopardyBoard, their existing score is preserved.
     */
    public synchronized void syncLatePlayer(String name) {
        if (name == null || name.isBlank()) return;
        String clean = name.trim();
        List<IPlayer> players = jeopardyBoard.getPlayers();
        for (IPlayer p : players) {
            if (p.getNames().trim().equalsIgnoreCase(clean)) {
                return; // Existing profile and score preserved!
            }
        }
        // If the game has already advanced past the setup / registration menus, add the contestant to the active board
        if (currentMenu != null && currentMenu != startMenu && currentMenu != playerMenu && currentMenu != loadMenu && currentMenu != questionCreator) {
            players.add(new JeopardyPlayer(clean));
            System.out.println("Screen: Late-joining contestant added to game: " + clean);
        }
    }

    /**
     * Fixed-timestep 60 FPS game loop for continuous frame ticking and rendering.
     */
    @Override
    public void run() {
        long drawInterval = 1000000000L / FPS;
        long nextDrawTime = System.nanoTime() + drawInterval;

        long fpsTimer = System.nanoTime();
        int frameCount = 0;

        while (isVisible()) {
            long currentTime = System.nanoTime();
            update();
            repaint();

            frameCount++;
            if (currentTime - fpsTimer >= 1000000000L) {
                fpsCount = frameCount;
                frameCount = 0;
                fpsTimer = currentTime;
            }

            try {
                long remainingTime = (nextDrawTime - System.nanoTime()) / 1000000L;
                if (remainingTime < 0) {
                    remainingTime = 0;
                }
                Thread.sleep(remainingTime);
                nextDrawTime += drawInterval;
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void update() {
        validate();
        allMenus.forEach(AbstractMenu::update);
    }
}
