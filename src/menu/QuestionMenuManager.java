package menu;

import impl.JeopardyQuestion;
import intr.IQuestion;
import net.BuzzerServer;
import screen.Screen;
import util.AudioPlayer;
import util.JBackgroundPanel;
import util.JButtonListener;
import util.MediaResolver;
import util.configure.ButtonConfigure;
import util.configure.TextConfigure;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;

/**
 * Question and Answer presentation screen.
 *
 * <p>Major functionalities:
 * <ul>
 *   <li>Presents the question text with auto-wrapping</li>
 *   <li>Dynamically renders scaled question images (if present)</li>
 *   <li>Provides play/stop controls for MP3 audio clips via {@link AudioPlayer}</li>
 *   <li>Integrates with {@link BuzzerServer} for atomic mobile buzzing</li>
 *   <li>Allows the host to mark an answer incorrect, locking out that contestant and reopening the buzzer</li>
 *   <li>Reveals the answer and navigates to the {@link RewardMenuManager} to allocate points</li>
 * </ul>
        * </p>
        */
        public class QuestionMenuManager extends AbstractMenu implements Consumer<IQuestion> {
            private JButtonListener answerButton;
    private JButtonListener nextButton;

    private JLabel question;
    private JLabel answer;
    private JLabel imageLabel;

    private JPanel audioPanel;
    private JButtonListener playAudioButton;
    private JLabel audioStatusLabel;

    private JPanel buzzerPanel;
    private JLabel buzzerStatusLabel;
    private JButtonListener wrongAnswerBtn;
    private JButtonListener reopenBtn;

    private boolean isAnswerRevealed = false;
    private IQuestion currentQuestion;

    public QuestionMenuManager(Screen screen) {
        super(screen);
    }

    @Override
    public void onReset() {
        AudioPlayer.stop();
        BuzzerServer.getInstance().closeBuzzer();
        if (imageLabel != null) {
            imageLabel.setIcon(null);
            imageLabel.setVisible(false);
        }
        if (audioPanel != null) {
            audioPanel.setVisible(false);
        }
    }

    @Override
    public void onRemove() {
        AudioPlayer.stop();
        BuzzerServer.getInstance().closeBuzzer();
    }

    @Override
    public JPanel addBackground() {
        return new JBackgroundPanel("background_questions.jpg");
    }

    @Override
    public JPanel addForeground() {
        JPanel fg = new JPanel();

        question = new JLabel();
        answer = new JLabel();
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setVisible(false);

        currentQuestion = new JeopardyQuestion("Default question", "Default answer", 0);

        // Row 0: Question Label (Initially visible)
        TextConfigure.configure()
                .setX(0).setY(0)
                .setText(currentQuestion.getQuestion())
                .setBackgroundColor(Screen.JEOPARDY_BLUE)
                .setSize(20).setStyle(Font.BOLD)
                .setWidth(600).setHeight(50)
                .setNorth(15).setSouth(10)
                .confirm(question, screen, fg);

        // Row 0: Answer Label (Initially hidden, revealed on "Show Answer")
        TextConfigure.configure()
                .setX(0).setY(0)
                .setText(currentQuestion.getAnswer())
                .setInvisible()
                .setBackgroundColor(Screen.JEOPARDY_BLUE)
                .setSize(20).setStyle(Font.BOLD)
                .setWidth(600).setHeight(50)
                .setNorth(15).setSouth(10)
                .confirm(answer, screen, fg);

        // Row 1: Optional Picture Clue
        GridBagConstraints gbcImage = new GridBagConstraints();
        gbcImage.gridx = 0;
        gbcImage.gridy = 1;
        gbcImage.insets = new Insets(5, 10, 10, 10);
        gbcImage.anchor = GridBagConstraints.CENTER;
        screen.getLayout().setConstraints(imageLabel, gbcImage);
        fg.add(imageLabel);

        // Row 2: Optional Audio Clue Player
        audioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        audioPanel.setOpaque(false);

        playAudioButton = new JButtonListener("Play Audio Clip", (e) -> {
            if (currentQuestion == null || currentQuestion.getAudio() == null) {
                return;
            }
            if (AudioPlayer.isPlaying()) {
                AudioPlayer.stop();
                playAudioButton.setText("Play Audio Clip");
                playAudioButton.setBackground(Color.WHITE);
                audioStatusLabel.setText("Audio stopped");
            } else {
                playAudioButton.setText("Stop Audio");
                playAudioButton.setBackground(new Color(255, 100, 100));
                audioStatusLabel.setText("Playing audio clip...");
                AudioPlayer.play(currentQuestion.getAudio(), () -> {
                    SwingUtilities.invokeLater(() -> {
                        playAudioButton.setText("Replay Audio");
                        playAudioButton.setBackground(Color.WHITE);
                        audioStatusLabel.setText("Audio finished");
                    });
                });
            }
        });
        playAudioButton.setFont(new Font("Century Gothic", Font.BOLD, 14));
        playAudioButton.setBackground(Color.WHITE);

        audioStatusLabel = new JLabel("Audio clip ready");
        audioStatusLabel.setFont(new Font("Century Gothic", Font.BOLD, 14));
        audioStatusLabel.setForeground(Screen.JEOPARDY_YELLOW);

        audioPanel.add(playAudioButton);
        audioPanel.add(audioStatusLabel);
        audioPanel.setVisible(false);

        GridBagConstraints gbcAudio = new GridBagConstraints();
        gbcAudio.gridx = 0;
        gbcAudio.gridy = 2;
        gbcAudio.insets = new Insets(5, 10, 10, 10);
        gbcAudio.anchor = GridBagConstraints.CENTER;
        screen.getLayout().setConstraints(audioPanel, gbcAudio);
        fg.add(audioPanel);

        // Row 3: Mobile Buzzer Status Bar & Host Controls
        buzzerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 6));
        buzzerPanel.setOpaque(true);
        buzzerPanel.setBackground(Screen.JEOPARDY_BLUE);

        buzzerStatusLabel = new JLabel("Mobile Buzzer: READY");
        buzzerStatusLabel.setFont(new Font("Century Gothic", Font.BOLD, 16));
        buzzerStatusLabel.setForeground(Screen.JEOPARDY_YELLOW);
        buzzerPanel.add(buzzerStatusLabel);

        wrongAnswerBtn = new JButtonListener("Incorrect (Reopen Buzzer)", (e) -> {
            BuzzerServer.getInstance().markCurrentWinnerWrong();
        });
        wrongAnswerBtn.setFont(new Font("Century Gothic", Font.BOLD, 13));
        wrongAnswerBtn.setBackground(new Color(255, 100, 100));
        wrongAnswerBtn.setForeground(Color.WHITE);
        wrongAnswerBtn.setVisible(false);
        buzzerPanel.add(wrongAnswerBtn);

        reopenBtn = new JButtonListener("Reopen All", (e) -> {
            BuzzerServer.getInstance().resetBuzzer();
        });
        reopenBtn.setFont(new Font("Century Gothic", Font.BOLD, 13));
        reopenBtn.setBackground(Color.WHITE);
        reopenBtn.setForeground(Color.BLACK);
        reopenBtn.setVisible(false);
        buzzerPanel.add(reopenBtn);

        GridBagConstraints gbcBuzzer = new GridBagConstraints();
        gbcBuzzer.gridx = 0;
        gbcBuzzer.gridy = 3;
        gbcBuzzer.insets = new Insets(8, 10, 8, 10);
        gbcBuzzer.anchor = GridBagConstraints.CENTER;
        screen.getLayout().setConstraints(buzzerPanel, gbcBuzzer);
        fg.add(buzzerPanel);

        // Row 4: Action Buttons (Show Answer -> Next)
        answerButton = new JButtonListener("Show Answer", (e) -> {
            AudioPlayer.stop();
            isAnswerRevealed = true;
            BuzzerServer.getInstance().setAnswerRevealed(true);
            answerButton.setVisible(false);
            nextButton.setVisible(true);
            question.setVisible(false);
            answer.setVisible(true);
        });

        nextButton = new JButtonListener("Next =>", (e) -> {
            AudioPlayer.stop();
            isAnswerRevealed = false;
            BuzzerServer.getInstance().setAnswerRevealed(false);
            BuzzerServer.getInstance().closeBuzzer();
            answerButton.setVisible(true);
            nextButton.setVisible(false);
            question.setVisible(true);
            answer.setVisible(false);

            if (screen.getRewardMenu() instanceof RewardMenuManager rewardMenu) {
                rewardMenu.accept(currentQuestion);
            }

            screen.setContentPane(screen.getRewardMenu());
        });

        ButtonConfigure.configure()
                .setX(0).setY(4)
                .setWidth(75).setHeight(40)
                .setNorth(15).setSouth(15)
                .finish(answerButton, screen, fg);

        ButtonConfigure.configure()
                .setX(0).setY(4)
                .setWidth(75).setHeight(40)
                .setNorth(15).setSouth(15)
                .finish(nextButton, screen, fg);

        return fg;
    }

    @Override
    public void onSetActive() {
        AudioPlayer.stop();
        isAnswerRevealed = false;
        BuzzerServer.getInstance().setAnswerRevealed(false);

        answerButton.setVisible(true);
        nextButton.setVisible(false);
        question.setVisible(true);
        answer.setVisible(false);

        if (currentQuestion == null) {
            return;
        }

        // Open mobile buzzer for this question
        BuzzerServer.getInstance().openBuzzer(currentQuestion);

        // Setup question and answer text with auto-wrapping
        String qText = "<html><center style='width: 600px;'>" + currentQuestion.getQuestion() + "</center></html>";
        String aText = "<html><center style='width: 600px;'>" + currentQuestion.getAnswer() + "</center></html>";
        question.setText(qText);
        answer.setText(aText);

        // Load and scale clue image if present
        String imgPath = currentQuestion.getImage();
        if (imgPath != null && !imgPath.isBlank()) {
            File imgFile = MediaResolver.resolveMediaFile(imgPath);
            if (imgFile != null && imgFile.exists()) {
                try {
                    BufferedImage bimg = ImageIO.read(imgFile);
                    if (bimg != null) {
                        int maxW = 460;
                        int maxH = 280;
                        int origW = bimg.getWidth();
                        int origH = bimg.getHeight();
                        double scale = Math.min((double) maxW / origW, (double) maxH / origH);
                        if (scale > 1.0) scale = 1.0;
                        int targetW = Math.max(1, (int) (origW * scale));
                        int targetH = Math.max(1, (int) (origH * scale));
                        Image scaled = bimg.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
                        imageLabel.setIcon(new ImageIcon(scaled));
                        imageLabel.setVisible(true);
                    } else {
                        imageLabel.setIcon(null);
                        imageLabel.setVisible(false);
                    }
                } catch (Exception e) {
                    System.err.println("QuestionMenuManager: Error loading image: " + e.getMessage());
                    imageLabel.setIcon(null);
                    imageLabel.setVisible(false);
                }
            } else {
                imageLabel.setIcon(null);
                imageLabel.setVisible(false);
            }
        } else {
            imageLabel.setIcon(null);
            imageLabel.setVisible(false);
        }

        // Configure audio controls if sound clue is present
        String audioPath = currentQuestion.getAudio();
        if (audioPath != null && !audioPath.isBlank()) {
            audioPanel.setVisible(true);
            playAudioButton.setText("Play Audio Clip");
            playAudioButton.setBackground(Color.WHITE);
            audioStatusLabel.setText("Audio clip ready");
        } else {
            audioPanel.setVisible(false);
        }

        updateBuzzerUI();
    }

    @Override
    public void tick() {
        updateBuzzerUI();
    }

    /**
     * Synchronizes the buzzer status label and control buttons with BuzzerServer state.
     * Displays accurate game state during clue presentation and answer reveal.
     */
    private void updateBuzzerUI() {
        if (currentQuestion == null || buzzerStatusLabel == null) return;

        if (!currentQuestion.isBuzzable()) {
            if (isAnswerRevealed) {
                buzzerStatusLabel.setText("Answer Revealed");
                buzzerStatusLabel.setForeground(Screen.JEOPARDY_YELLOW);
            } else {
                buzzerStatusLabel.setText("Competitive Round - Buzzer Disabled");
                buzzerStatusLabel.setForeground(Color.LIGHT_GRAY);
            }
            wrongAnswerBtn.setVisible(false);
            reopenBtn.setVisible(false);
            return;
        }

        // When the answer is displayed, never show "Waiting for buzz..."
        if (isAnswerRevealed) {
            String winner = BuzzerServer.getInstance().getLastSuccessfulWinner();
            if (winner != null && !winner.isBlank()) {
                buzzerStatusLabel.setText("Answer Revealed | Contestant: " + winner);
                buzzerStatusLabel.setForeground(new Color(80, 255, 80));
            } else {
                buzzerStatusLabel.setText("Answer Revealed - No Contestant Buzzed");
                buzzerStatusLabel.setForeground(Screen.JEOPARDY_YELLOW);
            }
            wrongAnswerBtn.setVisible(false);
            reopenBtn.setVisible(false);
            return;
        }

        String winner = BuzzerServer.getInstance().getCurrentBuzzerWinner();
        if (winner != null) {
            buzzerStatusLabel.setText(winner + " buzzed in!");
            buzzerStatusLabel.setForeground(new Color(80, 255, 80));
            wrongAnswerBtn.setVisible(true);
            reopenBtn.setVisible(true);
        } else if (BuzzerServer.getInstance().isBuzzerOpen()) {
            buzzerStatusLabel.setText("Waiting for buzz...");
            buzzerStatusLabel.setForeground(Screen.JEOPARDY_YELLOW);
            wrongAnswerBtn.setVisible(false);
            reopenBtn.setVisible(false);
        } else {
            buzzerStatusLabel.setText("Buzzer Closed - Waiting for Host");
            buzzerStatusLabel.setForeground(Color.LIGHT_GRAY);
            wrongAnswerBtn.setVisible(false);
            reopenBtn.setVisible(true);
        }
    }

    @Override
    public void accept(IQuestion question) {
        this.currentQuestion = question;
    }
}
