package intr;

public interface IQuestionFactory<T extends IQuestion> {
    T create(String question, String answer, int value);

    default T create(String question, String answer, int value, String image, String audio, String tileText) {
        return create(question, answer, value);
    }

    default T create(String question, String answer, int value, String image, String audio, String tileText, boolean buzzable) {
        return create(question, answer, value, image, audio, tileText);
    }
}
