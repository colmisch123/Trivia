package impl;

import intr.IQuestion;

/**
 * Immutable data model representing a single Jeopardy question clue.
 *
 * <p>Supports:
 * <ul>
 *   <li>Question clue text and correct answer text</li>
 *   <li>Base point value (e.g. 100, 200, or negative for special penalty tiles)</li>
 *   <li>Optional visual clue image path/filename (e.g. {@code r.jpg})</li>
 *   <li>Optional audio clip path/filename (e.g. {@code burp.mp3})</li>
 *   <li>Optional tile label override (e.g. "DO NOT CHOOSE THIS TILE")</li>
 *   <li>{@code buzzable} flag indicating whether mobile buzzing is enabled (defaults to {@code true}; {@code false} for competitive rounds)</li>
 * </ul>
 * </p>
 */
public class JeopardyQuestion implements IQuestion {
    private final String question;
    private final String answer;
    private final int value;
    private final String image;
    private final String audio;
    private final String tileText;
    private final boolean buzzable;

    public JeopardyQuestion(String question, String answer, int value) {
        this(question, answer, value, null, null, null, true);
    }

    public JeopardyQuestion(String question, String answer, int value, String image, String audio, String tileText) {
        this(question, answer, value, image, audio, tileText, true);
    }

    public JeopardyQuestion(String question, String answer, int value, String image, String audio, String tileText, boolean buzzable) {
        this.question = question;
        this.answer = answer;
        this.value = value;
        this.image = image;
        this.audio = audio;
        this.tileText = tileText;
        this.buzzable = buzzable;
    }

    @Override
    public String getQuestion() {
        return question;
    }

    @Override
    public String getAnswer() {
        return answer;
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public String getImage() {
        return image;
    }

    @Override
    public String getAudio() {
        return audio;
    }

    @Override
    public String getTileText() {
        return tileText;
    }

    @Override
    public boolean isBuzzable() {
        return buzzable;
    }

    @Override
    public String toString() {
        return getQuestion() + " " + getAnswer() + " Value: " + getValue();
    }
}
