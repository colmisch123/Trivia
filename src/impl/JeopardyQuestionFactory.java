package impl;

import intr.IQuestion;
import intr.IQuestionFactory;

public class JeopardyQuestionFactory implements IQuestionFactory<IQuestion> {
    @Override
    public IQuestion create(String question, String answer, int value) {
        return new JeopardyQuestion(question, answer, value);
    }

    @Override
    public IQuestion create(String question, String answer, int value, String image, String audio, String tileText) {
        return new JeopardyQuestion(question, answer, value, image, audio, tileText);
    }

    @Override
    public IQuestion create(String question, String answer, int value, String image, String audio, String tileText, boolean buzzable) {
        return new JeopardyQuestion(question, answer, value, image, audio, tileText, buzzable);
    }

    public static JeopardyQuestionFactory getFactory() {
        return new JeopardyQuestionFactory();
    }
}
