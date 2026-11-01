package intr;

/**
 * Interface contract for a Jeopardy question clue.
 */
public interface IQuestion {
    String getQuestion();
    String getAnswer();
    int getValue();

    default String getImage() {
        return null;
    }

    default String getAudio() {
        return null;
    }

    default String getTileText() {
        return null;
    }

    default boolean isBuzzable() {
        return true;
    }
}
