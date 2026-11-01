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

/**
 * Handles serialization, deserialization, file I/O, and validation for Jeopardy game boards in JSON format.
 *
 * <h3>JSON Structure Overview:</h3>
 * <pre>{@code
 * [
 *   {
 *     "subject_title": "Science",
 *     "subject_questions": [
 *       {
 *         "question": "What is H2O?",
 *         "answer": "Water",
 *         "value": 100,
 *         "image": "r.jpg",          // Optional: picture path
 *         "audio": "burp.mp3",       // Optional: sound clip path
 *         "tile_text": "SPECIAL",    // Optional: override text on board tile
 *         "buzzable": false          // Optional: false for competitive challenges (default: true)
 *       }
 *     ]
 *   }
 * ]
 * }</pre>
 */
public class JsonHandler {
    // JSON property keys
    private static final String TITLE = "subject_title";
    private static final String SUBJECT = "subject_questions";
    private static final String QUESTION = "question";
    private static final String ANSWER = "answer";
    private static final String VALUE = "value";
    private static final String IMAGE = "image";
    private static final String AUDIO = "audio";
    private static final String TILE_TEXT = "tile_text";
    private static final String BUZZABLE = "buzzable";

    private static final String DIRECTORY = System.getProperty("user.dir");

    /**
     * Encapsulates the result of checking a board JSON file before loading it.
     */
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

    /**
     * Normalizes board filenames: removes leading path prefixes, replaces spaces with underscores,
     * and guarantees a {@code .json} extension.
     */
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

    /**
     * Serializes an entire {@link IBoard} into pretty-printed JSON.
     */
    public static <T extends ISubject> String serializeBoard(IBoard<T> board) {
        return serializeSubjects(board.getSubjects());
    }

    /**
     * Serializes a list of {@link ISubject} categories and their questions into pretty-printed JSON.
     */
    public static <T extends ISubject> String serializeSubjects(List<T> subjects) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray entireJson = new JsonArray();

        for (T s : subjects) {
            JsonObject jsonSubject = new JsonObject();
            jsonSubject.addProperty(TITLE, s.getTitle());
            JsonArray questionJson = new JsonArray();

            List<IQuestion> questions = s.getQuestions();
            for (IQuestion q : questions) {
                JsonObject questionObject = new JsonObject();
                questionObject.addProperty(QUESTION, q.getQuestion());
                questionObject.addProperty(ANSWER, q.getAnswer());
                questionObject.addProperty(VALUE, q.getValue());

                if (q.getImage() != null && !q.getImage().isBlank()) {
                    questionObject.addProperty(IMAGE, q.getImage());
                }
                if (q.getAudio() != null && !q.getAudio().isBlank()) {
                    questionObject.addProperty(AUDIO, q.getAudio());
                }
                if (q.getTileText() != null && !q.getTileText().isBlank()) {
                    questionObject.addProperty(TILE_TEXT, q.getTileText());
                }
                if (!q.isBuzzable()) {
                    questionObject.addProperty(BUZZABLE, false);
                }
                questionJson.add(questionObject);
            }

            jsonSubject.add(SUBJECT, questionJson);
            entireJson.add(jsonSubject);
        }

        return gson.toJson(entireJson);
    }

    /**
     * Deserializes a JSON string into a list of categories (subjects) using the provided factories.
     */
    public static <T extends ISubject, Q extends IQuestion> List<T> deSerializeSubjects(
            @NotNull String json,
            @NotNull ISubjectFactory<T> subjectFactory,
            @NotNull IQuestionFactory<Q> questionFactory) {

        JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
        List<T> subjects = new ArrayList<>(jsonArray.size());

        for (JsonElement e : jsonArray) {
            JsonObject jsonObject = e.getAsJsonObject();
            String title = jsonObject.has(TITLE) ? jsonObject.get(TITLE).getAsString() : "Untitled";
            T subject = subjectFactory.create(title);

            if (jsonObject.has(SUBJECT) && jsonObject.get(SUBJECT).isJsonArray()) {
                JsonArray questions = jsonObject.getAsJsonArray(SUBJECT);
                for (JsonElement q : questions) {
                    JsonObject qObject = q.getAsJsonObject();
                    String question = qObject.has(QUESTION) ? qObject.get(QUESTION).getAsString() : "";
                    String answer = qObject.has(ANSWER) ? qObject.get(ANSWER).getAsString() : "";
                    int value = qObject.has(VALUE) ? qObject.get(VALUE).getAsInt() : 0;
                    String image = qObject.has(IMAGE) && !qObject.get(IMAGE).isJsonNull() ? qObject.get(IMAGE).getAsString() : null;
                    String audio = qObject.has(AUDIO) && !qObject.get(AUDIO).isJsonNull() ? qObject.get(AUDIO).getAsString() : null;
                    String tileText = qObject.has(TILE_TEXT) && !qObject.get(TILE_TEXT).isJsonNull() ? qObject.get(TILE_TEXT).getAsString() : null;
                    boolean buzzable = true;
                    if (qObject.has(BUZZABLE) && !qObject.get(BUZZABLE).isJsonNull()) {
                        buzzable = qObject.get(BUZZABLE).getAsBoolean();
                    }

                    subject.addQuestion(questionFactory.create(question, answer, value, image, audio, tileText, buzzable));
                }
            }

            subjects.add(subject);
        }

        return subjects;
    }

    /**
     * Exports raw JSON string to a file inside the {@code SavedBoards} directory.
     */
    public static void exportJsonToFile(@NotNull String json, @NotNull String fileName) {
        String file = normalizeFileName(fileName);

        File directory = new File(DIRECTORY + "/SavedBoards");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (FileWriter fileWriter = new FileWriter(new File(directory, file), StandardCharsets.UTF_8)) {
            fileWriter.write(json);
            fileWriter.flush();
        } catch (IOException e) {
            System.err.println("JsonHandler: Error exporting JSON to " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Reads the contents of a board file from the {@code SavedBoards} directory or classpath resources.
     */
    @Nullable
    public static String readJsonFromFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        String normalized = normalizeFileName(fileName);

        List<File> candidates = List.of(
            new File(DIRECTORY + "/SavedBoards/" + normalized),
            new File("SavedBoards/" + normalized),
            new File(DIRECTORY + "/" + normalized),
            new File(normalized),
            new File(fileName)
        );

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                try {
                    return Files.readString(candidate.toPath(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    System.err.println("JsonHandler: Error reading file: " + candidate.getAbsolutePath());
                }
            }
        }

        // Fallback to classpath resources if bundled in a JAR
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

    /**
     * Validates whether a file exists and contains valid, non-empty Jeopardy board JSON data.
     */
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

    /**
     * Scans the {@code SavedBoards} directory and returns all {@code .json} board filenames.
     */
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
