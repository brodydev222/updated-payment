package com.paytracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Settings stored in .minecraft/config/paytracker.json */
public class Config {
    private static final int CURRENT_VERSION = 2;

    // Group 1 = player name, group 2 = number, group 3 = optional K/M/B/T suffix
    private static final String AMT = "\\$?\\s*([\\d,]+(?:\\.\\d+)?)\\s*([kKmMbBtT])?(?!\\w).*";
    public static final String DEFAULT_RECEIVED = "^\\W*(?:\\[[^\\]]*\\]\\s*)*([\\w.]{1,17}) (?:has |just )?(?:paid|sent|gave|pays|sends) you " + AMT;
    public static final String DEFAULT_SENT = "^\\W*You (?:have |just )?(?:paid|sent|gave) ([\\w.]{1,17}) " + AMT;

    public int version = 0;
    public String receivedPattern = DEFAULT_RECEIVED;
    public String sentPattern = DEFAULT_SENT;
    /** Your balance when you started tracking. Net worth = this + received - sent. */
    public double startingBalance = 0;
    public boolean hudVisible = true;
    public boolean debug = false;
    public int hudX = 6;
    public int hudY = 6;
    public int timerSeconds = 30;

    public transient Pattern received;
    public transient Pattern sent;

    public static Config INSTANCE = new Config();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("paytracker.json");
    }

    public static void load() {
        try {
            if (Files.exists(file())) {
                Config c = GSON.fromJson(Files.readString(file()), Config.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception e) {
            System.err.println("[PayTracker] Could not read config: " + e);
        }
        if (INSTANCE.version < CURRENT_VERSION) {
            // upgrade from an older version: use the new default patterns and turn debug on
            INSTANCE.receivedPattern = DEFAULT_RECEIVED;
            INSTANCE.sentPattern = DEFAULT_SENT;
            INSTANCE.debug = true;
            INSTANCE.version = CURRENT_VERSION;
        }
        INSTANCE.compile();
        save();
    }

    public void compile() {
        try { received = Pattern.compile(receivedPattern); }
        catch (PatternSyntaxException e) { received = Pattern.compile(DEFAULT_RECEIVED); }
        try { sent = Pattern.compile(sentPattern); }
        catch (PatternSyntaxException e) { sent = Pattern.compile(DEFAULT_SENT); }
    }

    public static void save() {
        try { Files.writeString(file(), GSON.toJson(INSTANCE)); }
        catch (Exception e) { System.err.println("[PayTracker] Could not save config: " + e); }
    }
}
