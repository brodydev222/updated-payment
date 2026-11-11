package com.paytracker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class PayTrackerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        PaymentTracker.loadData();

        // Server messages (chat lines and the action bar).
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> PaymentTracker.onMessage(message, overlay));
        // Messages that arrive as "chat" but have no player sender (some servers do this for plugin messages).
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) -> {
            if (sender == null) PaymentTracker.onMessage(message, false);
        });

        HudRenderCallback.EVENT.register(HudOverlay::render);

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("paytracker", "main"));
        KeyBinding openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paytracker.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        KeyBinding toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paytracker.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PaymentTracker.tick();
            while (openMenu.wasPressed()) {
                client.setScreen(new TrackerScreen());
            }
            while (toggleHud.wasPressed()) {
                Config.INSTANCE.hudVisible = !Config.INSTANCE.hudVisible;
                Config.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Ocean overlay: " + (Config.INSTANCE.hudVisible ? "ON" : "OFF")), true);
                }
            }
        });
    }
}
