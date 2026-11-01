package intr;

import java.util.List;

/**
 * Interface contract for a Jeopardy category (subject) containing question clues.
 */
public interface ISubject {
    String getTitle();
    List<IQuestion> getQuestions();

    void setTitle(String title);
    void setQuestions(List<IQuestion> questions);

    default void addQuestion(IQuestion question) {
        getQuestions().add(question);
    }
}
