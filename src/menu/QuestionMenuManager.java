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

        // Row 0: Question Label
        TextConfigure.configure()
                .setX(0).setY(0)
                .setText(currentQuestion.getQuestion())
                .setBackgroundColor(Screen.JEOPARDY_BLUE)
                .setSize(20).setStyle(Font.BOLD)
                .setWidth(600).setHeight(50)
                .setNorth(15).setSouth(10)
                .confirm(question, screen, fg);

        // Row 0 (Overlaid initially invisible): Answer Label
        TextConfigure.configure()
                .setX(0).setY(0)
                .setText(currentQuestion.getAnswer())
                .setInvisible()
                .setBackgroundColor(Screen.JEOPARDY_BLUE)
                .setSize(20).setStyle(Font.BOLD)
                .setWidth(600).setHeight(50)
                .setNorth(15).setSouth(10)
                .confirm(answer, screen, fg);

        // Row 1: Image Label
        GridBagConstraints gbcImage = new GridBagConstraints();
        gbcImage.gridx = 0;
        gbcImage.gridy = 1;
        gbcImage.insets = new Insets(5, 10, 10, 10);
        gbcImage.anchor = GridBagConstraints.CENTER;
        screen.getLayout().setConstraints(imageLabel, gbcImage);
        fg.add(imageLabel);

        // Row 2: Audio Panel
        audioPanel = new JPanel();
        audioPanel.setOpaque(false);
        audioPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));

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

        // Row 3: Buzzer Status & Control Bar
        buzzerPanel = new JPanel();
        buzzerPanel.setOpaque(true);
        buzzerPanel.setBackground(Screen.JEOPARDY_BLUE);
        buzzerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 6));

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

        // Row 4: Action Buttons (Show Answer, Next)
        answerButton = new JButtonListener("Show Answer", (e) -> {
            AudioPlayer.stop();
            BuzzerServer.getInstance().closeBuzzer();
            answerButton.setVisible(false);
            nextButton.setVisible(true);
            question.setVisible(false);
            answer.setVisible(true);
        });

        nextButton = new JButtonListener("Next =>", (e) -> {
            AudioPlayer.stop();
            BuzzerServer.getInstance().closeBuzzer();
            answerButton.setVisible(true);
            nextButton.setVisible(false);
            question.setVisible(true);
            answer.setVisible(false);

            if(screen.getRewardMenu() instanceof Consumer setCurrentQuestion) {
                setCurrentQuestion.accept(currentQuestion);
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

        // Setup image if available
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
                    System.err.println("Error loading question image: " + e.getMessage());
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

        // Setup audio controls if available
        String audioPath = currentQuestion.getAudio();
        if (audioPath != null && !audioPath.isBlank()) {
            audioPanel.setVisible(true);
            playAudioButton.setText("Play Audio Clip");
            playAudioButton.setBackground(Color.WHITE);
            audioStatusLabel.setText("Audio clip ready");
        } else {
            audioPanel.setVisible(false);
        }

        // Setup initial buzzer state
        updateBuzzerUI();
    }

    @Override
    public void tick() {
        updateBuzzerUI();
    }

    private void updateBuzzerUI() {
        if (currentQuestion == null || buzzerStatusLabel == null) return;

        if (!currentQuestion.isBuzzable()) {
            buzzerStatusLabel.setText("Competitive Round - Buzzer Disabled");
            buzzerStatusLabel.setForeground(Color.LIGHT_GRAY);
            wrongAnswerBtn.setVisible(false);
            reopenBtn.setVisible(false);
            return;
        }

        String winner = BuzzerServer.getInstance().getCurrentBuzzerWinner();
        if (winner != null) {
            buzzerStatusLabel.setText("BUZZ! " + winner.toUpperCase() + " buzzed in!");
            buzzerStatusLabel.setForeground(new Color(80, 255, 80));
            wrongAnswerBtn.setVisible(true);
            reopenBtn.setVisible(true);
        } else {
            buzzerStatusLabel.setText("Mobile Buzzer: READY - Waiting for buzz...");
            buzzerStatusLabel.setForeground(Screen.JEOPARDY_YELLOW);
            wrongAnswerBtn.setVisible(false);
            reopenBtn.setVisible(false);
        }
    }

    // For setting question
    @Override
    public void accept(IQuestion iQuestion) {
        this.currentQuestion = iQuestion;
    }
}
