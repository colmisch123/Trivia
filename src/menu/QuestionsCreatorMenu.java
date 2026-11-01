package menu;

import impl.JeopardyQuestion;
import impl.JeopardySubject;
import impl.JeopardyQuestionFactory;
import impl.JeopardySubjectFactory;
import intr.IQuestion;
import intr.ISubject;
import screen.Screen;
import util.JBackgroundPanel;
import util.JButtonListener;
import util.JsonHandler;
import util.Pair;
import util.configure.ButtonConfigure;
import util.configure.TextConfigure;
import util.configure.TextFieldConfigure;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom board builder and editor menu.
 *
 * <p>Allows users to:
 * <ul>
 *   <li>Create new categories (subjects)</li>
 *   <li>Add, edit (via paginated view), and delete questions within categories</li>
 *   <li>Load existing board JSON files to modify them</li>
 *   <li>Export and save the customized board to the {@code SavedBoards/} directory</li>
 * </ul>
 * </p>
 */
public class QuestionsCreatorMenu extends AbstractMenu {
    private final List<ISubject> subjects = new ArrayList<>();
    private final List<Pair<ISubject, JLabel>> allSubjectsAndLabels = new ArrayList<>();
    private final List<JPanel> allPanels = new ArrayList<>();

    // Sub-panels for multi-step editing flows
    private final JPanel createSubjectPanel;
    private final JPanel topButtons;
    private final JPanel subjectAddQuestion;
    private final JPanel subjectEdit;
    private final JPanel openFile;
    private final JPanel confirmBoard;

    private boolean hasDisplayedError = false;
    private boolean hasEditDisplayedError = false;

    // Pagination index when browsing questions in edit mode
    private int editIndex = 0;

    // Y coordinate tracker for laying out subjects in the foreground list
    private int subjectY = 1;

    public QuestionsCreatorMenu(Screen screen) {
        super(screen);

        createSubjectPanel = new JPanel();
        initPanel(createSubjectPanel, false, JLayeredPane.MODAL_LAYER);

        topButtons = new JPanel();
        initPanel(topButtons, true, JLayeredPane.DRAG_LAYER);

        subjectAddQuestion = new JPanel();
        initPanel(subjectAddQuestion, false, JLayeredPane.POPUP_LAYER);

        subjectEdit = new JPanel();
        initPanel(subjectEdit, false, JLayeredPane.POPUP_LAYER);

        openFile = new JPanel();
        initPanel(openFile, false, JLayeredPane.POPUP_LAYER);

        confirmBoard = new JPanel();
        initPanel(confirmBoard, false, JLayeredPane.POPUP_LAYER);

        setupCreateSubject();
        setupTopButtons();
        setupConfirmBoard();
        setupOpen();

        allPanels.add(createSubjectPanel);
        allPanels.add(foreground);
        allPanels.add(subjectAddQuestion);
        allPanels.add(subjectEdit);
        allPanels.add(openFile);
        allPanels.add(confirmBoard);
    }

    @Override
    public void onReset() {
        subjectY = 1;
        subjects.clear();
        allSubjectsAndLabels.clear();
        foreground.removeAll();
        screen.setFileToOpen("Halloween_Trivia.json");
        setupForeground();
        foreground.revalidate();
        foreground.repaint();
    }

    @Override
    public JPanel addBackground() {
        return new JBackgroundPanel("question_creator.jpg");
    }

    @Override
    public JPanel addForeground() {
        foreground = new JPanel();
        setupForeground();
        return foreground;
    }

    @Override
    public void tick() {
        allPanels.forEach(panel -> panel.setBounds(0, 0, width, height));
    }

    /**
     * Shows the specified panel and hides all other creator sub-panels.
     */
    private void setPanelVisible(JPanel panel) {
        refreshSubjectQuestionSizes();
        allPanels.forEach(p -> p.setVisible(p == panel));
    }

    private void initPanel(JPanel panel, boolean visible, Integer layer) {
        panel.setOpaque(false);
        panel.setVisible(visible);
        panel.setLayout(screen.getLayout());
        panel.setBounds(0, 0, width, height);
        pane.add(panel, layer);
    }

    /**
     * Initializes the default foreground view showing the list of categories and the Add Subject button.
     */
    private void setupForeground() {
        JLabel createSubject = new JLabel();

        JButtonListener addSubject = new JButtonListener("Add Subject", (e) -> {
            setPanelVisible(createSubjectPanel);
        });

        ButtonConfigure.configure()
                .setWidth(100).setHeight(15)
                .setX(1).setY(0)
                .finish(addSubject, screen, foreground);

        TextConfigure.configure()
                .setText("Create Subjects:")
                .setSize(30).setStyle(Font.BOLD)
                .setEast(50)
                .confirm(createSubject, screen, foreground);
    }

    /**
     * Persistent navigation bar: Exit to Start Menu, Reset, Open file, and Save board.
     */
    private void setupTopButtons() {
        JButtonListener exit = new JButtonListener((e) -> screen.setContentPane(screen.getStartMenu()));
        JButtonListener reset = new JButtonListener((e) -> onReset());
        JButtonListener open = new JButtonListener((e) -> setPanelVisible(openFile));
        JButtonListener confirm = new JButtonListener((e) -> setPanelVisible(confirmBoard));

        ButtonConfigure.configure().setX(0).setText("Exit").setSouth(600).finish(exit, screen, topButtons);
        ButtonConfigure.configure().setX(1).setText("Reset").setSouth(600).finish(reset, screen, topButtons);
        ButtonConfigure.configure().setX(2).setText("Open").setWest(25).setSouth(600).finish(open, screen, topButtons);
        ButtonConfigure.configure().setX(3).setText("Confirm").setSouth(600).setEast(650).finish(confirm, screen, topButtons);
    }

    /**
     * Sub-panel for creating a new category by name.
     */
    private void setupCreateSubject() {
        JTextField enterTitle = new JTextField();
        JLabel enterLabel = new JLabel();

        JButtonListener confirm = new JButtonListener("Confirm", (e) -> {
            String title = enterTitle.getText().trim();
            if (title.isBlank()) title = "Untitled Subject";
            ISubject createdSubject = new JeopardySubject(title);
            subjects.add(createdSubject);
            onSubjectCreated(createdSubject);
            enterTitle.setText("");
            setPanelVisible(foreground);
        });

        JButtonListener back = new JButtonListener("Back", (e) -> setPanelVisible(foreground));

        TextConfigure.configure().setStyle(Font.BOLD).setY(0).setSouth(50).setSize(50)
                .setText("Subject Name: ").confirm(enterLabel, screen, createSubjectPanel);

        TextFieldConfigure.configure().setColumn(25).setWidth(100).setHeight(25).setText("Enter Subject Name")
                .setX(0).setY(1).setSize(15).confirm(enterTitle, screen, createSubjectPanel);

        ButtonConfigure.configure().setY(1).setWidth(40).setHeight(20).setNorth(175).finish(confirm, screen, createSubjectPanel);
        ButtonConfigure.configure().setY(2).setWidth(30).setHeight(10).setNorth(25).finish(back, screen, createSubjectPanel);
    }

    /**
     * Called when a category is added to the list: attaches Add Question, Edit, and Delete action buttons.
     */
    private void onSubjectCreated(ISubject subjectCreated) {
        JLabel newSubject = new JLabel();

        JButtonListener addQuestion = new JButtonListener("Add Question", (e) -> {
            onCreateQuestion(subjectCreated);
            setPanelVisible(subjectAddQuestion);
        });

        JButtonListener edit = new JButtonListener("Edit", (e) -> {
            onSubjectEdit(subjectCreated);
            setPanelVisible(subjectEdit);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addQuestion);
        buttonPanel.add(edit);

        JButtonListener delete = new JButtonListener("Delete", (e) -> {
            subjects.remove(subjectCreated);
            foreground.remove(newSubject);
            foreground.remove(buttonPanel);
            allSubjectsAndLabels.removeIf(pair -> pair.first() == subjectCreated);
            foreground.revalidate();
            foreground.repaint();
        });
        buttonPanel.add(delete);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = subjectY;
        gbc.fill = GridBagConstraints.NONE;
        foreground.add(buttonPanel, gbc);

        TextConfigure.configure().setX(0).setY(subjectY)
                .setText(subjectCreated.getTitle() + " Qs: " + subjectCreated.getQuestions().size())
                .setSize(20).confirm(newSubject, screen, foreground, GridBagConstraints.WEST);

        allSubjectsAndLabels.add(new Pair<>(subjectCreated, newSubject));
        subjectY++;
    }

    private void refreshSubjectQuestionSizes() {
        for (Pair<ISubject, JLabel> pair : allSubjectsAndLabels) {
            ISubject subject = pair.first();
            JLabel label = pair.second();
            label.setText(subject.getTitle() + " Qs: " + subject.getQuestions().size());
        }
    }

    /**
     * Sub-panel for entering Question text, Answer text, and Point Value.
     */
    private void onCreateQuestion(ISubject subjectToAddTo) {
        topButtons.setVisible(false);
        subjectAddQuestion.removeAll();

        JLabel subject = new JLabel();
        JLabel question = new JLabel();
        JLabel answer = new JLabel();
        JLabel value = new JLabel();

        JTextField questionField = new JTextField();
        JTextField answerField = new JTextField();
        JTextField valueField = new JTextField("100");

        JButtonListener confirm = new JButtonListener("Confirm", (e) -> {
            try {
                int qValue = Integer.parseInt(valueField.getText().trim());
                subjectToAddTo.addQuestion(new JeopardyQuestion(questionField.getText(), answerField.getText(), qValue));
                refreshSubjectQuestionSizes();
                topButtons.setVisible(true);
                subjectAddQuestion.removeAll();
                hasDisplayedError = false;
                setPanelVisible(foreground);
            } catch (NumberFormatException exception) {
                if (!hasDisplayedError) {
                    hasDisplayedError = true;
                    JLabel error = new JLabel();
                    TextConfigure.configure().setText("        Value has to be an Integer")
                            .setTextColor(Color.red).setY(3).setX(2).setSize(10).setStyle(Font.BOLD)
                            .confirm(error, screen, subjectAddQuestion);
                    subjectAddQuestion.revalidate();
                    subjectAddQuestion.repaint();
                }
            }
        });

        JButtonListener back = new JButtonListener("Back", (e) -> {
            setPanelVisible(foreground);
            topButtons.setVisible(true);
            subjectAddQuestion.removeAll();
        });

        TextConfigure.configure().setText("For \"" + subjectToAddTo.getTitle() + "\":").setY(0).setSize(25).setStyle(Font.BOLD).confirm(subject, screen, subjectAddQuestion);
        TextConfigure.configure().setText("Question: ").setY(1).setSize(25).setStyle(Font.BOLD).confirm(question, screen, subjectAddQuestion);
        TextConfigure.configure().setText("Answer: ").setY(2).setSize(25).setStyle(Font.BOLD).setWidth(20).setHeight(20).confirm(answer, screen, subjectAddQuestion);
        TextConfigure.configure().setText("Value: $").setY(3).setSize(25).setStyle(Font.BOLD).setWidth(20).setHeight(20).confirm(value, screen, subjectAddQuestion);

        TextFieldConfigure.configure().setX(1).setSize(10).setHeight(30).setWidth(200).setY(1).confirm(questionField, screen, subjectAddQuestion);
        TextFieldConfigure.configure().setX(1).setSize(10).setHeight(30).setWidth(200).setY(2).confirm(answerField, screen, subjectAddQuestion);
        TextFieldConfigure.configure().setText("100").setX(1).setSize(10).setHeight(30).setWidth(200).setY(3).confirm(valueField, screen, subjectAddQuestion);

        ButtonConfigure.configure().setY(4).finish(back, screen, subjectAddQuestion);
        ButtonConfigure.configure().setX(1).setY(4).finish(confirm, screen, subjectAddQuestion);
    }

    /**
     * Paginated editor for reviewing and editing existing questions in a category.
     */
    private void onSubjectEdit(ISubject subjectToEdit) {
        topButtons.setVisible(false);
        subjectEdit.removeAll();

        int total = subjectToEdit.getQuestions().size();
        if (total == 0) {
            exitEdit();
            return;
        }

        if (editIndex >= total) editIndex = total - 1;
        if (editIndex < 0) editIndex = 0;

        IQuestion currentQuestion = subjectToEdit.getQuestions().get(editIndex);

        JLabel subject = new JLabel();
        JLabel question = new JLabel();
        JLabel answer = new JLabel();
        JLabel value = new JLabel();
        JLabel pageTracker = new JLabel();
        JLabel oldLabel = new JLabel();
        JLabel newLabel = new JLabel();

        JTextField questionField = new JTextField();
        JTextField answerField = new JTextField();
        JTextField valueField = new JTextField();

        JButtonListener exit = new JButtonListener("Exit Edit", (e) -> {
            saveEdit(questionField, answerField, valueField, subjectToEdit, 0);
            if (!hasEditDisplayedError) {
                exitEdit();
            }
        });

        JButtonListener back = new JButtonListener("Back", (e) -> {
            if (editIndex > 0) {
                saveEdit(questionField, answerField, valueField, subjectToEdit, -1);
            }
        });

        JButtonListener next = new JButtonListener("Next", (e) -> {
            if (editIndex + 1 < subjectToEdit.getQuestions().size()) {
                saveEdit(questionField, answerField, valueField, subjectToEdit, 1);
            }
        });

        JButtonListener delete = new JButtonListener("Delete", (e) -> {
            subjectToEdit.getQuestions().remove(currentQuestion);
            if (subjectToEdit.getQuestions().isEmpty()) {
                exitEdit();
            } else {
                if (editIndex >= subjectToEdit.getQuestions().size()) {
                    editIndex = subjectToEdit.getQuestions().size() - 1;
                }
                onSubjectEdit(subjectToEdit);
            }
        });

        TextConfigure.configure().setText("For \"" + subjectToEdit.getTitle() + "\":").setY(0).setSize(25).setStyle(Font.BOLD).confirm(subject, screen, subjectEdit);
        TextConfigure.configure().setText("Page: " + (editIndex + 1) + "/" + total).setY(0).setX(1).setSize(25).setStyle(Font.BOLD).setWidth(20).setHeight(20).confirm(pageTracker, screen, subjectEdit);

        TextConfigure.configure().setText("Current: ").setY(1).setSize(25).setStyle(Font.BOLD).confirm(oldLabel, screen, subjectEdit);
        TextConfigure.configure().setText("New: ").setY(1).setX(1).setSize(25).setStyle(Font.BOLD).confirm(newLabel, screen, subjectEdit);

        TextConfigure.configure().setText("\"" + currentQuestion.getQuestion() + "\"   ").setY(2).setSize(25).setStyle(Font.BOLD).confirm(question, screen, subjectEdit);
        TextConfigure.configure().setText("\"" + currentQuestion.getAnswer() + "\"   ").setY(3).setSize(25).setStyle(Font.BOLD).setWidth(20).setHeight(20).confirm(answer, screen, subjectEdit);
        TextConfigure.configure().setText("\"" + currentQuestion.getValue() + "\"   ").setY(4).setSize(25).setStyle(Font.BOLD).setWidth(20).setHeight(20).confirm(value, screen, subjectEdit);

        TextFieldConfigure.configure().setText(currentQuestion.getQuestion()).setX(1).setSize(10).setHeight(30).setWidth(200).setY(2).confirm(questionField, screen, subjectEdit);
        TextFieldConfigure.configure().setText(currentQuestion.getAnswer()).setX(1).setSize(10).setHeight(30).setWidth(200).setY(3).confirm(answerField, screen, subjectEdit);
        TextFieldConfigure.configure().setText(String.valueOf(currentQuestion.getValue())).setX(1).setSize(10).setHeight(30).setWidth(200).setY(4).confirm(valueField, screen, subjectEdit);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        navPanel.setOpaque(false);
        navPanel.add(back);
        navPanel.add(next);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        subjectEdit.add(navPanel, gbc);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(delete);
        actionPanel.add(exit);

        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridx = 1;
        gbc2.gridy = 5;
        subjectEdit.add(actionPanel, gbc2);
    }

    private void exitEdit() {
        subjectEdit.removeAll();
        setPanelVisible(foreground);
        topButtons.setVisible(true);
        editIndex = 0;
    }

    private void saveEdit(JTextField questionField, JTextField answerField, JTextField valueField, ISubject subjectToEdit, int addToIndex) {
        List<IQuestion> questions = subjectToEdit.getQuestions();

        try {
            int qValue = Integer.parseInt(valueField.getText().trim());
            questions.set(editIndex, new JeopardyQuestion(questionField.getText(), answerField.getText(), qValue));
            subjectEdit.removeAll();
            hasEditDisplayedError = false;
            editIndex += addToIndex;
            onSubjectEdit(subjectToEdit);
        } catch (NumberFormatException exception) {
            if (!hasEditDisplayedError) {
                hasEditDisplayedError = true;
                JLabel error = new JLabel();
                TextConfigure.configure().setText("        Value has to be an Integer")
                        .setTextColor(Color.red).setY(4).setX(2).setSize(10).setStyle(Font.BOLD)
                        .confirm(error, screen, subjectEdit);
                subjectEdit.revalidate();
                subjectEdit.repaint();
            }
        }
    }

    /**
     * Sub-panel for loading an existing board JSON file.
     */
    private void setupOpen() {
        JLabel enter = new JLabel();
        TextConfigure.configure().setText("Enter Jeopardy Board To Open:").setSize(30).setStyle(Font.BOLD).setWidth(100).setSouth(25).confirm(enter, screen, openFile);

        JLabel currentLoadedFile = new JLabel();
        TextConfigure.configure().setText("").setY(3).setStyle(Font.BOLD).setSize(15).setWidth(100).setNorth(100).confirm(currentLoadedFile, screen, openFile);

        JTextField enterName = new JTextField();
        TextFieldConfigure.configure().setText(screen.getFileToOpen()).setSize(15).setY(1)
                .setHeight(25).setWidth(500).setStyle(Font.BOLD)
                .setNorth(10).setEast(10).setSouth(10).setWest(10).confirm(enterName, screen, openFile);

        JButtonListener open = new JButtonListener((e) -> {
            String fileName = enterName.getText().trim();
            String board = JsonHandler.readJsonFromFile(fileName);
            if (board == null) {
                JOptionPane.showMessageDialog(screen, "Could not find board file: " + fileName, "Error Opening File", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                List<ISubject> subjectsIO = JsonHandler.deSerializeSubjects(board, JeopardySubjectFactory.getFactory(), JeopardyQuestionFactory.getFactory());
                subjects.clear();
                allSubjectsAndLabels.clear();
                foreground.removeAll();
                setupForeground();
                subjectY = 1;

                for (ISubject s : subjectsIO) {
                    subjects.add(s);
                    onSubjectCreated(s);
                }
                screen.setFileToOpen(fileName);
                currentLoadedFile.setText(screen.getFileToOpen() + " Loaded");
                setPanelVisible(foreground);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(screen, "Failed to parse board file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButtonListener back = new JButtonListener((e) -> {
            setPanelVisible(foreground);
            currentLoadedFile.setText("");
        });

        ButtonConfigure.configure().setX(0).setY(1).setWidth(90).setHeight(25).setNorth(125).setText("Open").setWest(10).finish(open, screen, openFile);
        ButtonConfigure.configure().setX(0).setY(2).setWidth(60).setHeight(25).setNorth(25).setText("Back").setWest(10).finish(back, screen, openFile);
    }

    /**
     * Sub-panel for saving the current board configuration to a JSON file.
     */
    private void setupConfirmBoard() {
        JLabel enter = new JLabel();
        TextConfigure.configure().setText("Enter Jeopardy Board Name:").setSize(30).setStyle(Font.BOLD).setWidth(100).setSouth(25).confirm(enter, screen, confirmBoard);

        JLabel currentLoadedFile = new JLabel();
        TextConfigure.configure().setText("").setY(3).setStyle(Font.BOLD).setSize(15).setWidth(100).setNorth(100).confirm(currentLoadedFile, screen, confirmBoard);

        JTextField enterName = new JTextField();
        TextFieldConfigure.configure().setText("custom_board.json").setSize(15).setY(1)
                .setHeight(25).setWidth(500).setStyle(Font.BOLD)
                .setNorth(10).setEast(10).setSouth(10).setWest(10).confirm(enterName, screen, confirmBoard);

        JButtonListener confirm = new JButtonListener((e) -> {
            String name = enterName.getText().trim();
            if (name.isBlank()) name = "custom_board.json";
            screen.setFileToOpen(name);
            JsonHandler.exportJsonToFile(JsonHandler.serializeSubjects(this.subjects), screen.getFileToOpen());
            currentLoadedFile.setText(screen.getFileToOpen() + " Saved");
            JOptionPane.showMessageDialog(screen, "Board successfully saved as " + screen.getFileToOpen(), "Board Saved", JOptionPane.INFORMATION_MESSAGE);
            setPanelVisible(foreground);
        });

        JButtonListener back = new JButtonListener((e) -> {
            setPanelVisible(foreground);
            currentLoadedFile.setText("");
        });

        ButtonConfigure.configure().setX(0).setY(1).setWidth(90).setHeight(25).setNorth(125).setText("Save").setWest(10).finish(confirm, screen, confirmBoard);
        ButtonConfigure.configure().setX(0).setY(2).setWidth(60).setHeight(25).setNorth(25).setText("Back").setWest(10).finish(back, screen, confirmBoard);
    }
}
