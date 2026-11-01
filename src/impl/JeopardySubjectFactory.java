package impl;

import intr.ISubject;
import intr.ISubjectFactory;

/**
 * Factory for instantiating {@link JeopardySubject} category instances.
 */
public class JeopardySubjectFactory implements ISubjectFactory<ISubject> {

    @Override
    public ISubject create(String title) {
        return new JeopardySubject(title);
    }

    public static JeopardySubjectFactory getFactory() {
        return new JeopardySubjectFactory();
    }
}
