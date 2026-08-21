package dev.christine.compassenhanced.client.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class SearchHistoryStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("compass_enhanced/search_history");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int FILE_VERSION = 1;
    private static final int MAX_ENTRIES = 8;
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("compass_enhanced-search-history.json");
    private static final SearchHistoryStore INSTANCE = new SearchHistoryStore();

    private final List<Identifier> entries = new ArrayList<>();

    private SearchHistoryStore() {
        load();
    }

    public static SearchHistoryStore getInstance() {
        return INSTANCE;
    }

    public synchronized List<Identifier> entries() {
        return List.copyOf(entries);
    }

    public synchronized void recordAttempt(Identifier itemId) {
        if (!isValidItem(itemId)) {
            return;
        }
        if (!entries.isEmpty() && entries.getFirst().equals(itemId)) {
            return;
        }

        entries.remove(itemId);
        entries.addFirst(itemId);
        if (entries.size() > MAX_ENTRIES) {
            entries.subList(MAX_ENTRIES, entries.size()).clear();
        }
        save();
    }

    private void load() {
        if (!Files.isRegularFile(FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                throw new IOException("History root must be an object");
            }

            JsonObject root = rootElement.getAsJsonObject();
            if (!root.has("version")
                    || !root.get("version").isJsonPrimitive()
                    || root.get("version").getAsInt() != FILE_VERSION
                    || !root.has("items")
                    || !root.get("items").isJsonArray()) {
                throw new IOException("Unsupported search history format");
            }

            List<Identifier> loaded = new ArrayList<>();
            for (JsonElement itemElement : root.getAsJsonArray("items")) {
                if (!itemElement.isJsonPrimitive() || !itemElement.getAsJsonPrimitive().isString()) {
                    continue;
                }
                Identifier itemId = Identifier.tryParse(itemElement.getAsString());
                if (isValidItem(itemId) && !loaded.contains(itemId)) {
                    loaded.add(itemId);
                    if (loaded.size() == MAX_ENTRIES) {
                        break;
                    }
                }
            }
            entries.addAll(loaded);
        } catch (Exception exception) {
            LOGGER.warn("Unable to load compass search history from {}", FILE, exception);
        }
    }

    private void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", FILE_VERSION);
        JsonArray items = new JsonArray();
        for (Identifier itemId : entries) {
            items.add(itemId.toString());
        }
        root.add("items", items);

        Path temporaryFile = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporaryFile = Files.createTempFile(FILE.getParent(), FILE.getFileName().toString(), ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            replaceFile(temporaryFile);
            temporaryFile = null;
        } catch (IOException exception) {
            LOGGER.warn("Unable to save compass search history to {}", FILE, exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException exception) {
                    LOGGER.debug("Unable to delete temporary search history file {}", temporaryFile, exception);
                }
            }
        }
    }

    private static void replaceFile(Path temporaryFile) throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    FILE,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isValidItem(Identifier itemId) {
        return itemId != null
                && Registries.ITEM.containsId(itemId)
                && Registries.ITEM.get(itemId) != Items.AIR;
    }
}
