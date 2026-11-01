package util;

import com.google.gson.*;
import impl.JeopardyQuestionFactory;
import impl.JeopardySubjectFactory;
import intr.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class JsonHandler {
    private static final String TITLE = "subject_title";
    private static final String SUBJECT = "subject_questions";
    private static final String QUESTION = "question";
    private static final String ANSWER = "answer";
    private static final String VALUE = "value";

    private static final String DIRECTORY = System.getProperty("user.dir");

    public static class BoardValidationResult {
        private final boolean valid;
        private final String message;
        private final String resolvedFileName;

        public BoardValidationResult(boolean valid, String message, String resolvedFileName) {
            this.valid = valid;
            this.message = message;
            this.resolvedFileName = resolvedFileName;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public String getResolvedFileName() {
            return resolvedFileName;
        }
    }

    public static String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        String clean = fileName.trim().replace('\\', '/');
        if (clean.toLowerCase().startsWith("savedboards/")) {
            clean = clean.substring("savedboards/".length());
        }
        clean = clean.replaceAll(" ", "_");
        if (!clean.toLowerCase().endsWith(".json")) {
            clean += ".json";
        }
        return clean;
    }

    public static <T extends ISubject> String serializeBoard(IBoard<T> board) {
        return serializeSubjects(board.getSubjects());
    }

    public static <T extends ISubject> String serializeSubjects(List<T> subjects) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray entireJson = new JsonArray();

        for(T s : subjects) {
            JsonObject jsonSubject = new JsonObject();
            jsonSubject.addProperty(TITLE, s.getTitle());
            JsonArray questionJson = new JsonArray();

            List<IQuestion> questions = s.getQuestions();
            for(IQuestion q : questions) {
                JsonObject questionObject = new JsonObject();
                questionObject.addProperty(QUESTION, q.getQuestion());
                questionObject.addProperty(ANSWER, q.getAnswer());
                questionObject.addProperty(VALUE, q.getValue());
                questionJson.add(questionObject);
            }

            jsonSubject.add(SUBJECT, questionJson);
            entireJson.add(jsonSubject);
        }

        return gson.toJson(entireJson);
    }

    public static <T extends ISubject, Q extends IQuestion> List<T> deSerializeSubjects(@NotNull String json, @NotNull ISubjectFactory<T> subjectFactory, @NotNull IQuestionFactory<Q> questionFactory) {
        JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

        List<T> subjects = new ArrayList<>(jsonArray.size());

        for(JsonElement e : jsonArray) {
            JsonObject jsonObject = e.getAsJsonObject();

            T subject = subjectFactory.create(jsonObject.get(TITLE).getAsString());

            JsonArray questions = jsonObject.getAsJsonArray(SUBJECT);
            for(JsonElement q : questions) {
                JsonObject qObject = q.getAsJsonObject();
                String question = qObject.get(QUESTION).getAsString();
                String answer = qObject.get(ANSWER).getAsString();
                int value = qObject.get(VALUE).getAsInt();
                subject.addQuestion(questionFactory.create(question, answer, value));
            }

            subjects.add(subject);
        }

        return subjects;
    }

    public static void exportJsonToFile(@NotNull String json, @NotNull String fileName) {
        String file = normalizeFileName(fileName);

        // Makes sure the directory exists
        File directory = new File(DIRECTORY + "/SavedBoards");
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + directory.getAbsolutePath());
            }
        }

        try(FileWriter fileWriter = new FileWriter(new File(directory, file))) {
            fileWriter.write(json);
            fileWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public static String readJsonFromFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        String normalized = normalizeFileName(fileName);

        List<File> candidates = new ArrayList<>();
        candidates.add(new File(DIRECTORY + "/SavedBoards/" + normalized));
        candidates.add(new File("SavedBoards/" + normalized));
        candidates.add(new File(DIRECTORY + "/" + normalized));
        candidates.add(new File(normalized));
        candidates.add(new File(fileName));

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                try {
                    return Files.readString(candidate.toPath(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    System.err.println("Error reading file: " + candidate.getAbsolutePath());
                }
            }
        }

        // Fallback to classpath resources if bundled
        String[] resourceCandidates = {
            "/SavedBoards/" + normalized,
            "/" + normalized,
            "SavedBoards/" + normalized,
            normalized
        };
        for (String rc : resourceCandidates) {
            try (InputStream is = JsonHandler.class.getResourceAsStream(rc)) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    public static BoardValidationResult validateBoardFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return new BoardValidationResult(false, "Please enter a board file name.", "");
        }

        String normalized = normalizeFileName(fileName);
        String json = readJsonFromFile(normalized);

        if (json == null || json.isBlank()) {
            return new BoardValidationResult(false, 
                "Board file '" + normalized + "' was not found in SavedBoards.", normalized);
        }

        try {
            List<ISubject> subjects = deSerializeSubjects(json, 
                JeopardySubjectFactory.getFactory(), 
                JeopardyQuestionFactory.getFactory());

            if (subjects == null || subjects.isEmpty()) {
                return new BoardValidationResult(false, 
                    "Board file '" + normalized + "' contains no categories.", normalized);
            }

            boolean hasQuestions = false;
            for (ISubject s : subjects) {
                if (s.getQuestions() != null && !s.getQuestions().isEmpty()) {
                    hasQuestions = true;
                    break;
                }
            }

            if (!hasQuestions) {
                return new BoardValidationResult(false, 
                    "Board file '" + normalized + "' has categories but no questions.", normalized);
            }

            return new BoardValidationResult(true, 
                "Board '" + normalized + "' loaded successfully!", normalized);
        } catch (Exception e) {
            return new BoardValidationResult(false, 
                "Board file '" + normalized + "' is not a valid Jeopardy board JSON format.", normalized);
        }
    }

    public static List<String> getAvailableBoardFiles() {
        List<String> files = new ArrayList<>();
        File dir = new File(DIRECTORY + "/SavedBoards");
        if (!dir.exists()) {
            dir = new File("SavedBoards");
        }
        if (dir.exists() && dir.isDirectory()) {
            File[] fileList = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
            if (fileList != null) {
                for (File f : fileList) {
                    files.add(f.getName());
                }
            }
        }
        return files;
    }
}
