package com.paytracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HudOverlay {
    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.INSTANCE.hudVisible || mc.options.hudHidden || mc.player == null) return;

        TextRenderer tr = mc.textRenderer;
        int x = Config.INSTANCE.hudX, y = Config.INSTANCE.hudY, w = 150, h = 70;

        double profit = PaymentTracker.todayProfit();

        Ocean.panel(ctx, x, y, w, h);
        ctx.drawTextWithShadow(tr, "TODAY'S PROFIT", x + 6, y + 6, Ocean.DIM);
        String bal = "Bal " + Format.money(PaymentTracker.netWorth());
        ctx.drawTextWithShadow(tr, bal, x + w - 6 - tr.getWidth(bal), y + 6, Ocean.ACCENT);

        ctx.drawTextWithShadow(tr, Text.literal(Format.signed(profit)).formatted(Formatting.BOLD),
                x + 6, y + 17, profit >= 0 ? Ocean.GOOD : Ocean.BAD);

        ctx.drawTextWithShadow(tr, "In " + Format.money(PaymentTracker.todayIn()), x + 6, y + 29, Ocean.GOOD);
        String out = "Out " + Format.money(PaymentTracker.todayOut());
        ctx.drawTextWithShadow(tr, out, x + w - 6 - tr.getWidth(out), y + 29, Ocean.BAD);

        Ocean.chart(ctx, x + 6, y + 41, w - 12, 22, PaymentTracker.todaySeries(), Ocean.ACCENT);

        // timer / auction card
        boolean showResult = PaymentTracker.hasResult && !PaymentTracker.timerRunning
                && System.currentTimeMillis() - PaymentTracker.timerEnd < 15000;
        if (PaymentTracker.timerRunning || showResult) {
            int y2 = y + h + 4, h2 = 32;
            Ocean.panel(ctx, x, y2, w, h2);
            Transaction top = PaymentTracker.topReceived;
            if (PaymentTracker.timerRunning) {
                ctx.drawTextWithShadow(tr, "AUCTION", x + 6, y2 + 6, Ocean.DIM);
                String t = Format.time(PaymentTracker.remainingMillis());
                ctx.drawTextWithShadow(tr, Text.literal(t).formatted(Formatting.BOLD),
                        x + w - 6 - tr.getWidth(t), y2 + 6, Ocean.ACCENT);
                String line = top == null ? "No payments yet" : "Top: " + top.player + " " + Format.money(top.amount);
                ctx.drawTextWithShadow(tr, line, x + 6, y2 + 18, top == null ? Ocean.DIM : Ocean.GOLD);
            } else {
                ctx.drawTextWithShadow(tr, "WINNER", x + 6, y2 + 6, Ocean.GOLD);
                String line = top == null ? "No payments received" : top.player + " " + Format.money(top.amount);
                ctx.drawTextWithShadow(tr, line, x + 6, y2 + 18, Ocean.TEXT);
            }
        }
    }
}
