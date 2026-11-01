package intr;

import java.util.List;

/**
 * Interface contract for managing the active board state: categories and contestants.
 *
 * @param <T> Category type extending {@link ISubject}.
 */
public interface IBoard<T extends ISubject> {
    List<T> getSubjects();
    List<IPlayer> getPlayers();

    void setSubjects(String fileToOpen);
    void clearAllPlayers();
}
