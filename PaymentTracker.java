package com.paytracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class PaymentTracker {
    private static final Gson GSON = new Gson();
    private static final int MAX_HISTORY = 1000;

    public static final List<Transaction> history = new ArrayList<>();
    public static double totalReceived = 0;
    public static double totalSent = 0;

    // ---- timer / auction ----
    public static boolean timerRunning = false;
    public static boolean hasResult = false;
    public static long timerStart = 0;
    public static long timerEnd = 0;
    public static long timerDurationMs = 30_000;
    public static Transaction topReceived = null; // the current leader / winner
    public static Transaction topSent = null;
    public static double timerTotal = 0;
    public static int timerCount = 0;

    // ---- de-duplication of chat vs action bar ----
    private static String lastChatRaw = "", lastOverlayRaw = "";
    private static long lastChatTime = 0, lastOverlayTime = 0;

    public static double netWorth() {
        return Config.INSTANCE.startingBalance + totalReceived - totalSent;
    }

    // ---------- today's numbers ----------

    private static long startOfToday() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static double todayIn() {
        long s = startOfToday(); double v = 0;
        for (Transaction t : history) if (t.time >= s && t.received) v += t.amount;
        return v;
    }

    public static double todayOut() {
        long s = startOfToday(); double v = 0;
        for (Transaction t : history) if (t.time >= s && !t.received) v += t.amount;
        return v;
    }

    public static double todayProfit() { return todayIn() - todayOut(); }

    /** Running profit total for today, starting at 0, for the chart. */
    public static double[] todaySeries() {
        long s = startOfToday();
        List<Double> pts = new ArrayList<>();
        double cum = 0;
        pts.add(0.0);
        for (Transaction t : history) {
            if (t.time >= s) { cum += t.received ? t.amount : -t.amount; pts.add(cum); }
        }
        double[] a = new double[pts.size()];
        for (int i = 0; i < a.length; i++) a[i] = pts.get(i);
        return a;
    }

    // ---------- chat parsing ----------

    public static void onMessage(Text message, boolean overlay) {
        String raw = message.getString().trim();
        if (raw.isEmpty()) return;
        long now = System.currentTimeMillis();

        // The same payment can show in chat AND the action bar (and the action bar can repeat). Count it once.
        if (overlay) {
            boolean repeat = raw.equals(lastOverlayRaw) && now - lastOverlayTime < 8000;
            boolean alsoInChat = raw.equals(lastChatRaw) && now - lastChatTime < 1500;
            lastOverlayRaw = raw; lastOverlayTime = now;
            if (repeat || alsoInChat) return;
        } else {
            boolean alsoOnBar = raw.equals(lastOverlayRaw) && now - lastOverlayTime < 1500;
            lastChatRaw = raw; lastChatTime = now;
            if (alsoOnBar) return;
        }

        String s = Format.normalize(raw);

        Matcher m = Config.INSTANCE.received.matcher(s);
        if (m.matches()) { record(true, m.group(1), parse(m)); return; }

        m = Config.INSTANCE.sent.matcher(s);
        if (m.matches()) { record(false, m.group(1), parse(m)); return; }

        if (Config.INSTANCE.debug) {
            String l = s.toLowerCase();
            if (l.contains("$") && (l.contains("paid") || l.contains("pay") || l.contains("sent") || l.contains("receiv") || l.contains("gave"))) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("[Ocean] Could not read" + (overlay ? " (action bar)" : "") + ": " + raw).formatted(Formatting.GRAY), false);
                }
            }
        }
    }

    private static double parse(Matcher m) {
        try {
            double v = Double.parseDouble(m.group(2).replace(",", ""));
            String suf = m.groupCount() >= 3 ? m.group(3) : null;
            return v * Format.mult(suf);
        } catch (Exception e) {
            return -1;
        }
    }

    private static void record(boolean received, String player, double amount) {
        if (amount < 0) return;
        Transaction t = new Transaction(System.currentTimeMillis(), received, player, amount);
        history.add(t);
        if (history.size() > MAX_HISTORY) history.remove(0);

        if (received) totalReceived += amount; else totalSent += amount;

        if (timerRunning) {
            if (received) {
                timerTotal += amount;
                timerCount++;
                if (topReceived == null || amount > topReceived.amount) topReceived = t;
            } else if (topSent == null || amount > topSent.amount) {
                topSent = t;
            }
        }
        saveData();
    }

    // ---------- timer ----------

    public static void startTimer(int seconds) {
        timerRunning = true;
        hasResult = false;
        timerStart = System.currentTimeMillis();
        timerDurationMs = seconds * 1000L;
        topReceived = null;
        topSent = null;
        timerTotal = 0;
        timerCount = 0;
    }

    /** Called every client tick. Ends the timer when the time is up. */
    public static void tick() {
        if (timerRunning && System.currentTimeMillis() - timerStart >= timerDurationMs) {
            stopTimer(true);
        }
    }

    public static void stopTimer(boolean announce) {
        if (!timerRunning) return;
        timerRunning = false;
        timerEnd = System.currentTimeMillis();
        hasResult = true;
        if (!announce) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            String msg = topReceived == null
                    ? "[Ocean] Time's up! No payments were received."
                    : "[Ocean] Time's up! Winner: " + topReceived.player + " with " + Format.money(topReceived.amount);
            mc.player.sendMessage(Text.literal(msg).formatted(Formatting.AQUA), false);
        }
    }

    public static long remainingMillis() {
        if (!timerRunning) return 0;
        return Math.max(0, timerDurationMs - (System.currentTimeMillis() - timerStart));
    }

    // ---------- persistence ----------

    private static Path dataFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("paytracker_data.json");
    }

    public static void loadData() {
        try {
            if (!Files.exists(dataFile())) return;
            List<Transaction> list = GSON.fromJson(Files.readString(dataFile()), new TypeToken<List<Transaction>>() {}.getType());
            if (list == null) return;
            history.clear();
            history.addAll(list);
            totalReceived = 0; totalSent = 0;
            for (Transaction t : history) {
                if (t.received) totalReceived += t.amount; else totalSent += t.amount;
            }
        } catch (Exception e) {
            System.err.println("[PayTracker] Could not load data: " + e);
        }
    }

    public static void saveData() {
        try { Files.writeString(dataFile(), GSON.toJson(history)); }
        catch (Exception e) { System.err.println("[PayTracker] Could not save data: " + e); }
    }

    public static void resetData() {
        history.clear();
        totalReceived = 0;
        totalSent = 0;
        saveData();
    }
}
