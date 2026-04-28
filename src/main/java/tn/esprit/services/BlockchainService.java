package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import tn.esprit.entities.User;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Blockchain Service — Pure Java, NO database.
 *
 * Each user has their own blockchain file stored locally:
 *   %APPDATA%/AutoLearn/blockchain_user_{id}.json   (Windows)
 *   ~/.autolearn/blockchain_user_{id}.json           (Linux/Mac)
 *
 * Each block contains:
 *   - index        : position in the chain
 *   - action       : what happened (PROFILE_UPDATE, PASSWORD_CHANGE, etc.)
 *   - data         : what changed (old/new values)
 *   - timestamp    : when it happened
 *   - previousHash : hash of the previous block  ← links the chain
 *   - hash         : SHA-256(index+action+data+timestamp+previousHash)
 *
 * Immutability guarantee:
 *   If anyone edits the JSON file and changes a block,
 *   its hash won't match what the next block expects → chain broken → detected.
 */
public class BlockchainService {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String GENESIS_HASH =
        "0000000000000000000000000000000000000000000000000000000000000000";

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Gson GSON =
        new GsonBuilder().setPrettyPrinting().create();

    // Storage directory: user home / .autolearn /
    private static final Path STORAGE_DIR = Path.of(
        System.getProperty("user.home"), ".autolearn"
    );

    // ── Block ─────────────────────────────────────────────────────────────────

    public static class Block {
        public int    index;
        public String action;
        public String data;        // JSON string of changes
        public String timestamp;
        public String previousHash;
        public String hash;

        public Block() {}

        public Block(int index, String action, String data,
                     String timestamp, String previousHash) {
            this.index        = index;
            this.action       = action;
            this.data         = data;
            this.timestamp    = timestamp;
            this.previousHash = previousHash;
            this.hash         = computeHash(index, action, data, timestamp, previousHash);
        }

        /** Human-readable label for UI */
        public String actionLabel() {
            return switch (action) {
                case "GENESIS"         -> "Compte cree";
                case "PROFILE_UPDATE"  -> "Informations modifiees";
                case "PASSWORD_CHANGE" -> "Mot de passe change";
                case "EMAIL_CHANGE"    -> "Email modifie";
                case "NIVEAU_CHANGE"   -> "Niveau modifie";
                case "LOGIN"           -> "Connexion";
                default                -> action;
            };
        }

        /** Verify this block's hash is still correct */
        public boolean isValid() {
            return computeHash(index, action, data, timestamp, previousHash).equals(hash);
        }
    }

    // ── Validation result ─────────────────────────────────────────────────────

    public record ValidationResult(
        boolean isValid,
        int     totalBlocks,
        int     invalidBlocks,
        String  message
    ) {}

    // ── File path ─────────────────────────────────────────────────────────────

    private static Path chainFile(int userId) {
        return STORAGE_DIR.resolve("blockchain_user_" + userId + ".json");
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    private static List<Block> loadChain(int userId) {
        Path file = chainFile(userId);
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<Block>>() {}.getType();
            List<Block> chain = GSON.fromJson(json, listType);
            return chain != null ? chain : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[Blockchain] Load error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static void saveChain(int userId, List<Block> chain) {
        try {
            Files.createDirectories(STORAGE_DIR);
            String json = GSON.toJson(chain);
            Files.writeString(chainFile(userId), json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.err.println("[Blockchain] Save error: " + e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Creates the genesis block (block #0) for a user.
     * Called once when the user first opens their profile.
     * Does nothing if genesis already exists.
     */
    public static void createGenesisBlock(int userId) {
        List<Block> chain = loadChain(userId);
        if (!chain.isEmpty()) return; // already exists

        Block genesis = new Block(
            0,
            "GENESIS",
            "{\"message\":\"Genesis block — Profile chain initialized\"}",
            LocalDateTime.now().format(FMT),
            GENESIS_HASH
        );
        chain.add(genesis);
        saveChain(userId, chain);
        System.out.println("[Blockchain] Genesis block created for user " + userId);
    }

    /**
     * Adds a new block to the user's chain.
     *
     * @param userId  the user
     * @param action  PROFILE_UPDATE / PASSWORD_CHANGE / EMAIL_CHANGE / NIVEAU_CHANGE
     * @param changes map of what changed (field → old/new values)
     */
    public static void addBlock(int userId, String action, Map<String, Object> changes) {
        List<Block> chain = loadChain(userId);

        // Ensure genesis exists
        if (chain.isEmpty()) {
            createGenesisBlock(userId);
            chain = loadChain(userId);
        }

        Block last = chain.get(chain.size() - 1);

        Block newBlock = new Block(
            last.index + 1,
            action,
            GSON.toJson(changes),
            LocalDateTime.now().format(FMT),
            last.hash
        );

        chain.add(newBlock);
        saveChain(userId, chain);

        System.out.println("[Blockchain] Block #" + newBlock.index
            + " added | action=" + action
            + " | hash=" + newBlock.hash.substring(0, 10) + "...");
    }

    /**
     * Returns all blocks for a user (ordered oldest → newest).
     */
    public static List<Block> getChain(int userId) {
        return loadChain(userId);
    }

    /**
     * Validates the entire chain:
     * 1. Each block's hash matches its computed hash  (data integrity)
     * 2. Each block's previousHash matches prev block (chain integrity)
     */
    public static ValidationResult validateChain(int userId) {
        List<Block> chain = loadChain(userId);

        if (chain.isEmpty()) {
            return new ValidationResult(true, 0, 0, "Aucun bloc trouvé");
        }

        int invalid = 0;

        for (int i = 0; i < chain.size(); i++) {
            Block b = chain.get(i);

            // Check 1: hash integrity
            if (!b.isValid()) {
                invalid++;
            }

            // Check 2: chain linkage (skip genesis)
            if (i > 0 && !b.previousHash.equals(chain.get(i - 1).hash)) {
                invalid++;
            }
        }

        boolean ok = invalid == 0;
        String msg = ok
            ? "Compte protege — " + chain.size() + " modification(s) enregistree(s)"
            : invalid + " probleme(s) detecte(s) dans l'historique";

        return new ValidationResult(ok, chain.size(), invalid, msg);
    }

    /**
     * Computes a security score 0–100 based on the chain.
     *
     * +20  chain exists
     * +30  chain is valid (not tampered)
     * +20  password was changed at least once
     * +15  more than 2 blocks (active user)
     * +15  no invalid blocks
     */
    public static int computeSecurityScore(int userId) {
        List<Block> chain = loadChain(userId);
        if (chain.isEmpty()) return 0;

        int score = 20; // chain exists

        ValidationResult v = validateChain(userId);
        if (v.isValid())  score += 30;
        if (v.invalidBlocks() == 0) score += 15;

        boolean pwdChanged = chain.stream()
            .anyMatch(b -> "PASSWORD_CHANGE".equals(b.action));
        if (pwdChanged) score += 20;

        if (chain.size() > 2) score += 15;

        return Math.min(score, 100);
    }

    // ── SHA-256 ───────────────────────────────────────────────────────────────

    /**
     * Core of the blockchain — links blocks together cryptographically.
     * SHA-256 is a native Java algorithm (java.security.MessageDigest).
     * No external dependency needed.
     */
    public static String computeHash(int index, String action, String data,
                                      String timestamp, String previousHash) {
        String input = index + action + (data != null ? data : "") + timestamp + previousHash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
