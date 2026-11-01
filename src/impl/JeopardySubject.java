package impl;

import intr.IQuestion;
import intr.ISubject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Jeopardy board category containing a title and an ordered list of questions.
 */
public class JeopardySubject implements ISubject {
    private String title;
    private List<IQuestion> questions;

    public JeopardySubject(String title) {
        this.title = title != null ? title.trim() : "Untitled";
        this.questions = new ArrayList<>();
    }

    public JeopardySubject(String title, List<IQuestion> questions) {
        this.title = title != null ? title.trim() : "Untitled";
        this.questions = questions != null ? questions : new ArrayList<>();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public List<IQuestion> getQuestions() {
        return questions;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void setQuestions(List<IQuestion> questions) {
        this.questions = questions;
    }

    @Override
    public String toString() {
        return title + " (" + questions.size() + " questions)";
    }
}
