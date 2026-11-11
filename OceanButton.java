package com.paytracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

/** A button drawn in the ocean theme. Click handling is vanilla. */
public class OceanButton extends ButtonWidget {
    public boolean selected = false;

    public OceanButton(int x, int y, int w, int h, String label, ButtonWidget.PressAction onPress) {
        // NOTE: ButtonWidget has an inner class called Text, so the real Text class is written out in full here.
        super(x, y, w, h, net.minecraft.text.Text.literal(label), onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    // The vanilla button background is drawn first; we paint over all of it with the ocean look.
    @Override
    public void drawIcon(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int bg = selected ? Ocean.BTN_ON : (isHovered() ? Ocean.BTN_HOVER : Ocean.BTN);
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        ctx.fill(x, y, x + w, y + h, selected ? Ocean.ACCENT : Ocean.BORDER_DIM);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String t = getMessage().getString();
        ctx.drawTextWithShadow(tr, t, x + (w - tr.getWidth(t)) / 2, y + (h - 8) / 2, Ocean.TEXT);
    }
}
