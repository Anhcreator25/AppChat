package chat.client.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple file‑based history manager for the Gemini bot.
 * Stores each line as "ROLE|CONTENT" where ROLE is USER or BOT.
 * The file is placed in the project working directory as ".bot_history".
 */
public class BotHistoryManager {
    private static final Path HISTORY_PATH = Path.of(System.getProperty("user.dir"), ".bot_history");

    public static List<String> load() {
        try {
            if (!Files.exists(HISTORY_PATH)) {
                return new ArrayList<>();
            }
            return Files.readAllLines(HISTORY_PATH);
        } catch (IOException e) {
            // On error return empty history – UI will simply show nothing
            return new ArrayList<>();
        }
    }

    public static void appendUser(String text) {
        appendLine("USER|" + text);
    }

    public static void appendBot(String text) {
        appendLine("BOT|" + text);
    }

    private static void appendLine(String line) {
        try {
            Files.writeString(HISTORY_PATH, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Silently ignore – UI will still work but history won't be persisted
        }
    }

    public static void clear() {
        try {
            Files.deleteIfExists(HISTORY_PATH);
        } catch (IOException ignored) {
        }
    }
}
