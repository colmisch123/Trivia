package intr;

/**
 * Interface contract for a contestant player.
 */
public interface IPlayer {
    String getNames();
    int getScore();
    void setScore(int amount);

    default void addScore(int amount) {
        setScore(getScore() + amount);
    }
}
