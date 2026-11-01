package impl;

import intr.IQuestion;

public class JeopardyQuestion implements IQuestion {
    private final String question;
    private final String answer;
    private final int value;
    private final String image;
    private final String audio;
    private final String tileText;

    public JeopardyQuestion(String question, String answer, int value) {
        this(question, answer, value, null, null, null);
    }

    public JeopardyQuestion(String question, String answer, int value, String image, String audio, String tileText) {
        this.question = question;
        this.answer = answer;
        this.value = value;
        this.image = image;
        this.audio = audio;
        this.tileText = tileText;
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
    public String toString() {
        return getQuestion() + " " + getAnswer() + " Value: " + getValue();
    }
}
