package com.paytracker;

import net.minecraft.client.gui.DrawContext;

/** Shared ocean-themed colors and drawing helpers. */
public class Ocean {
    public static final int BG = 0xEE06182A;
    public static final int HEADER = 0xFF0A2B44;
    public static final int PANEL2 = 0xFF0B2236;
    public static final int BORDER = 0xFF1E86A8;
    public static final int BORDER_DIM = 0xFF1B4F6B;
    public static final int ACCENT = 0xFF4FD8EB;
    public static final int TEXT = 0xFFE6F7FB;
    public static final int DIM = 0xFF7FB7C9;
    public static final int GOOD = 0xFF3DE0A0;
    public static final int BAD = 0xFFFF7A7A;
    public static final int GOLD = 0xFFFFD36B;
    public static final int BTN = 0xFF0F3A55;
    public static final int BTN_HOVER = 0xFF16587C;
    public static final int BTN_ON = 0xFF1B86B0;

    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + w, y + 1, BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h, BORDER);
        ctx.fill(x, y, x + 1, y + h, BORDER);
        ctx.fill(x + w - 1, y, x + w, y + h, BORDER);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, ACCENT); // bright top edge
        waves(ctx, x + 3, y + h - 4, w - 6);
    }

    /** A little row of wave marks. */
    public static void waves(DrawContext ctx, int x, int y, int w) {
        for (int i = 0; i + 3 <= w; i += 6) {
            int off = ((i / 6) % 2 == 0) ? 0 : 1;
            ctx.fill(x + i, y + off, x + i + 3, y + off + 1, 0x553A9BB8);
        }
    }

    /** Line chart with a faint filled area underneath. */
    public static void chart(DrawContext ctx, int x, int y, int w, int h, double[] s, int color) {
        if (w < 4 || h < 4) return;
        ctx.fill(x, y + h - 1, x + w, y + h, 0x402A6F8A); // baseline
        if (s == null || s.length < 2) {
            ctx.fill(x, y + h / 2, x + w, y + h / 2 + 1, 0x804FD8EB);
            return;
        }
        double min = s[0], max = s[0];
        for (double v : s) { min = Math.min(min, v); max = Math.max(max, v); }
        if (max - min < 1e-9) max = min + 1;
        int prevY = -1;
        for (int i = 0; i < w; i++) {
            double pos = (double) i / (w - 1) * (s.length - 1);
            int i0 = (int) Math.floor(pos);
            int i1 = Math.min(s.length - 1, i0 + 1);
            double f = pos - i0;
            double v = s[i0] + (s[i1] - s[i0]) * f;
            int py = y + (h - 1) - (int) Math.round((v - min) / (max - min) * (h - 1));
            int y1 = py, y2 = py;
            if (prevY >= 0) { y1 = Math.min(prevY, py); y2 = Math.max(prevY, py); }
            ctx.fill(x + i, y2 + 1, x + i + 1, y + h - 1, 0x204FD8EB); // area
            ctx.fill(x + i, y1, x + i + 1, y2 + 1, color);             // line
            prevY = py;
        }
    }
}
