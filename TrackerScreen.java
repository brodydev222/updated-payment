package com.paytracker;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class TrackerScreen extends Screen {
    private static final String[] TABS = {"Home", "Timer", "Profit", "Settings"};

    private int tab = 0;
    private String timerInput = String.valueOf(Config.INSTANCE.timerSeconds);
    private String balanceInput = "";
    private String message = "";
    private boolean confirmReset = false;

    private TextFieldWidget timeField;
    private TextFieldWidget balanceField;
    private OceanButton hudBtn;

    private int px, py, pw, ph, cx, cy, cw;
    private static final int SW = 84; // sidebar width

    public TrackerScreen() {
        super(Text.literal("Ocean Pay Tracker"));
    }

    private OceanButton add(int x, int y, int w, int h, String label, ButtonWidget.PressAction action) {
        return addDrawableChild(new OceanButton(x, y, w, h, label, action));
    }

    private void saveFields() {
        if (timeField != null) timerInput = timeField.getText();
        if (balanceField != null) balanceInput = balanceField.getText();
    }

    private String hudLabel() {
        return "Overlay: " + (Config.INSTANCE.hudVisible ? "ON" : "OFF");
    }

    @Override
    protected void init() {
        timeField = null;
        balanceField = null;

        pw = Math.min(this.width - 16, 400);
        ph = Math.min(this.height - 16, 240);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        cx = px + SW + 12;
        cy = py + 36;
        cw = pw - SW - 24;

        // header: overlay toggle + close
        hudBtn = add(px + pw - 118, py + 5, 92, 16, hudLabel(), b -> {
            Config.INSTANCE.hudVisible = !Config.INSTANCE.hudVisible;
            Config.save();
            b.setMessage(Text.literal(hudLabel()));
            hudBtn.selected = Config.INSTANCE.hudVisible;
        });
        hudBtn.selected = Config.INSTANCE.hudVisible;
        add(px + pw - 22, py + 5, 16, 16, "X", b -> close());

        // sidebar tabs
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            OceanButton t = add(px + 8, py + 36 + i * 26, SW - 8, 22, TABS[i], b -> {
                saveFields();
                tab = idx;
                confirmReset = false;
                message = "";
                clearAndInit();
            });
            t.selected = (i == tab);
        }

        if (tab == 1) buildTimerTab();
        if (tab == 2) buildProfitTab();
        if (tab == 3) buildSettingsTab();
    }

    private void buildTimerTab() {
        int[] presets = {30, 60, 120, 300};
        String[] labels = {"30s", "1m", "2m", "5m"};
        for (int i = 0; i < presets.length; i++) {
            final int sec = presets[i];
            OceanButton p = add(cx + i * 44, cy + 14, 40, 16, labels[i], b -> {
                saveFields();
                timerInput = String.valueOf(sec);
                clearAndInit();
            });
            p.selected = timerInput.trim().equals(String.valueOf(sec));
        }

        timeField = new TextFieldWidget(this.textRenderer, cx + 54, cy + 38, 70, 16, Text.literal("Seconds"));
        timeField.setMaxLength(8);
        timeField.setText(timerInput);
        addDrawableChild(timeField);

        add(cx, cy + 62, cw / 2 - 2, 18, "Start", b -> {
            int s = Format.parseSeconds(timeField.getText());
            if (s < 0) {
                message = "Enter seconds like 45, or 1:30 for minutes.";
            } else {
                Config.INSTANCE.timerSeconds = s;
                Config.save();
                timerInput = String.valueOf(s);
                PaymentTracker.startTimer(s);
                message = "";
            }
        });
        add(cx + cw / 2 + 2, cy + 62, cw / 2 - 2, 18, "Stop Now", b -> PaymentTracker.stopTimer(true));
    }

    private void buildProfitTab() {
        add(cx, py + ph - 30, 140, 16, confirmReset ? "Click again to confirm" : "Reset History", b -> {
            if (!confirmReset) {
                confirmReset = true;
                b.setMessage(Text.literal("Click again to confirm"));
            } else {
                PaymentTracker.resetData();
                confirmReset = false;
                b.setMessage(Text.literal("Reset History"));
                message = "History cleared.";
            }
        });
    }

    private void buildSettingsTab() {
        OceanButton ov = add(cx, cy + 4, 150, 18, hudLabel(), b -> {
            Config.INSTANCE.hudVisible = !Config.INSTANCE.hudVisible;
            Config.save();
            b.setMessage(Text.literal(hudLabel()));
            ((OceanButton) b).selected = Config.INSTANCE.hudVisible;
            hudBtn.setMessage(Text.literal(hudLabel()));
            hudBtn.selected = Config.INSTANCE.hudVisible;
        });
        ov.selected = Config.INSTANCE.hudVisible;

        OceanButton dbg = add(cx, cy + 28, 150, 18, "Debug: " + (Config.INSTANCE.debug ? "ON" : "OFF"), b -> {
            Config.INSTANCE.debug = !Config.INSTANCE.debug;
            Config.save();
            b.setMessage(Text.literal("Debug: " + (Config.INSTANCE.debug ? "ON" : "OFF")));
            ((OceanButton) b).selected = Config.INSTANCE.debug;
        });
        dbg.selected = Config.INSTANCE.debug;

        if (balanceInput.isEmpty() && Config.INSTANCE.startingBalance > 0) {
            balanceInput = Format.money(Config.INSTANCE.startingBalance).replace("$", "");
        }
        balanceField = new TextFieldWidget(this.textRenderer, cx, cy + 78, 100, 16, Text.literal("Balance"));
        balanceField.setMaxLength(16);
        balanceField.setText(balanceInput);
        addDrawableChild(balanceField);

        add(cx + 106, cy + 78, 60, 16, "Apply", b -> {
            double v = Format.parseAmount(balanceField.getText());
            if (v < 0) {
                message = "Enter an amount like 452B or 1.5M.";
            } else {
                Config.INSTANCE.startingBalance = v;
                Config.save();
                message = "Starting balance saved.";
            }
        });
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        Ocean.panel(ctx, px, py, pw, ph);
        ctx.fill(px + 1, py + 2, px + pw - 1, py + 28, Ocean.HEADER);
        ctx.fill(px + 1, py + 28, px + pw - 1, py + 29, Ocean.BORDER_DIM);
        ctx.fill(px + 4, py + 32, px + SW + 4, py + ph - 8, Ocean.PANEL2);
        ctx.fill(px + SW + 5, py + 32, px + SW + 6, py + ph - 8, Ocean.BORDER_DIM);
    }

    private void t(DrawContext c, String s, int x, int y, int col) {
        c.drawTextWithShadow(this.textRenderer, s, x, y, col);
    }

    private void bold(DrawContext c, String s, int x, int y, int col) {
        c.drawTextWithShadow(this.textRenderer, Text.literal(s).formatted(Formatting.BOLD), x, y, col);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        bold(ctx, "OCEAN PAY TRACKER", px + 10, py + 10, Ocean.ACCENT);

        switch (tab) {
            case 0 -> renderHome(ctx);
            case 1 -> renderTimer(ctx);
            case 2 -> renderProfit(ctx);
            case 3 -> renderSettings(ctx);
        }

        if (!message.isEmpty()) {
            t(ctx, message, cx, py + ph - 14, Ocean.GOLD);
        }
    }

    private void renderHome(DrawContext ctx) {
        int y = cy;
        t(ctx, "NET WORTH", cx, y, Ocean.DIM);
        bold(ctx, Format.money(PaymentTracker.netWorth()), cx, y + 11, Ocean.GOLD);

        double p = PaymentTracker.todayProfit();
        t(ctx, "TODAY'S PROFIT", cx, y + 30, Ocean.DIM);
        bold(ctx, Format.signed(p), cx, y + 41, p >= 0 ? Ocean.GOOD : Ocean.BAD);
        t(ctx, "In " + Format.money(PaymentTracker.todayIn()), cx + cw / 2, y + 11, Ocean.GOOD);
        t(ctx, "Out " + Format.money(PaymentTracker.todayOut()), cx + cw / 2, y + 41, Ocean.BAD);

        int ly = y + 62;
        t(ctx, "RECENT PAYMENTS", cx, ly, Ocean.DIM);
        List<Transaction> h = PaymentTracker.history;
        int maxLines = Math.max(0, (py + ph - 10 - (ly + 13)) / 10);
        if (h.isEmpty()) {
            t(ctx, "Nothing yet - waiting for a payment.", cx, ly + 13, Ocean.DIM);
        }
        for (int i = h.size() - 1, shown = 0; i >= 0 && shown < maxLines; i--, shown++) {
            Transaction tr = h.get(i);
            String s = tr.received
                    ? "+" + Format.money(tr.amount) + "  from " + tr.player
                    : "-" + Format.money(tr.amount) + "  to " + tr.player;
            t(ctx, s, cx, ly + 13 + shown * 10, tr.received ? Ocean.GOOD : Ocean.BAD);
        }
    }

    private void renderTimer(DrawContext ctx) {
        t(ctx, "AUCTION TIMER", cx, cy, Ocean.DIM);
        t(ctx, "Custom:", cx, cy + 42, Ocean.TEXT);
        t(ctx, "sec or m:ss", cx + 130, cy + 42, Ocean.DIM);

        int y = cy + 90;
        Transaction top = PaymentTracker.topReceived;
        if (PaymentTracker.timerRunning) {
            bold(ctx, "Time left: " + Format.time(PaymentTracker.remainingMillis()), cx, y, Ocean.ACCENT);
            if (top == null) {
                t(ctx, "No payments yet...", cx, y + 14, Ocean.DIM);
            } else {
                t(ctx, "Leading: " + top.player + "  " + Format.money(top.amount), cx, y + 14, Ocean.GOLD);
            }
            t(ctx, "Pot: " + Format.money(PaymentTracker.timerTotal) + " (" + PaymentTracker.timerCount + " payments)", cx, y + 26, Ocean.DIM);
        } else if (PaymentTracker.hasResult) {
            bold(ctx, "WINNER", cx, y, Ocean.GOLD);
            if (top == null) {
                t(ctx, "No payments were received.", cx, y + 14, Ocean.DIM);
            } else {
                t(ctx, top.player + "  -  " + Format.money(top.amount), cx, y + 14, Ocean.TEXT);
                t(ctx, "Pot: " + Format.money(PaymentTracker.timerTotal) + " (" + PaymentTracker.timerCount + " payments)", cx, y + 26, Ocean.DIM);
            }
        } else {
            t(ctx, "Pick a time and press Start.", cx, y, Ocean.DIM);
            t(ctx, "Highest single payment wins.", cx, y + 12, Ocean.DIM);
        }
    }

    private void renderProfit(DrawContext ctx) {
        double p = PaymentTracker.todayProfit();
        t(ctx, "TODAY", cx, cy, Ocean.DIM);
        bold(ctx, Format.signed(p), cx, cy + 11, p >= 0 ? Ocean.GOOD : Ocean.BAD);
        t(ctx, "In " + Format.money(PaymentTracker.todayIn()), cx + cw / 2, cy + 3, Ocean.GOOD);
        t(ctx, "Out " + Format.money(PaymentTracker.todayOut()), cx + cw / 2, cy + 15, Ocean.BAD);

        int chartH = Math.max(30, Math.min(70, ph - 140));
        Ocean.chart(ctx, cx, cy + 32, cw, chartH, PaymentTracker.todaySeries(), Ocean.ACCENT);

        int y = cy + 32 + chartH + 8;
        t(ctx, "All time received: +" + Format.money(PaymentTracker.totalReceived), cx, y, Ocean.GOOD);
        t(ctx, "All time sent: -" + Format.money(PaymentTracker.totalSent), cx, y + 11, Ocean.BAD);
    }

    private void renderSettings(DrawContext ctx) {
        t(ctx, "Starting balance (your balance right now):", cx, cy + 64, Ocean.DIM);
        t(ctx, "Net worth = starting balance + received - sent.", cx, cy + 100, Ocean.DIM);
        t(ctx, "Debug shows payment-like chat lines the mod", cx, cy + 116, Ocean.DIM);
        t(ctx, "could not read, so the pattern can be fixed.", cx, cy + 126, Ocean.DIM);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
