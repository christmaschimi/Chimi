package com.chimi.menu;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChimiMenuClient implements ClientModInitializer {
    public static final String ID = "chimismenu";
    public static final EconomyCore.Session session = new EconomyCore.Session();
    public static EconomyCore.Auction auction;
    public static ItemStack auctionStack = ItemStack.EMPTY;
    private static KeyMapping openKey;
    private static int tickCounter;
    private static final Pattern API_KEY = Pattern.compile("(?i)^\\s*api key is:?\\s*(\\S+)");

    @Override
    public void onInitializeClient() {
        Config.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
        openKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.chimismenu.open", InputConstants.Type.KEYSYM, InputConstants.KEY_G, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.gui.screen() == null) mc.gui.setScreen(new ChimiScreen());
            }
            BalanceTracker.tick(mc, session.isActive());
            if (++tickCounter % 10 == 0) BalanceTracker.readSidebar(mc, session.isActive());
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            boolean ok = screen instanceof InventoryScreen
                    || (Config.I.bgAllContainers && screen instanceof AbstractContainerScreen<?>);
            if (!ok) return;
            ScreenEvents.afterBackground(screen).register((scr, g, mx, my, delta) ->
                    BackgroundManager.draw(g, scr.width, scr.height));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BalanceTracker.resetAll());
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onMessage(message.getString()));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(ID, "profit"), ChimiMenuClient::drawHud);
    }

    public static void startSession() {
        session.start();
        BalanceTracker.beginSession(Minecraft.getInstance());
    }

    /** Real balance change when the API is connected, otherwise payments-only fallback. */
    public static boolean usingBalance() { return BalanceTracker.hasBalance(); }

    public static double profit() {
        return usingBalance() ? BalanceTracker.profit() : session.profit();
    }

    private static void onMessage(String text) {
        Matcher k = API_KEY.matcher(text);
        if (k.find()) {
            Config.I.apiKey = k.group(1);
            Config.save();
            return;
        }
        var payment = EconomyCore.parse(text);
        if (payment.isPresent()) {
            session.accept(payment.get());
            if (auction != null) auction.accept(payment.get());
            return;
        }
        BalanceTracker.acceptChat(Minecraft.getInstance(), text, session.isActive());
    }

    private static void drawHud(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!Config.I.hudEnabled || !session.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        double profit = profit();
        String line = "Profit: " + (profit >= 0 ? "+" : "") + EconomyCore.format(profit);
        int w = mc.font.width(line) + 8, h = 14;
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int c = Config.I.hudCorner;
        int x = (c == 1 || c == 3) ? sw - w - 4 : 4;
        int y = (c == 2 || c == 3) ? sh - h - 4 : 4;
        g.fill(x, y, x + w, y + h, 0x99000000);
        g.text(mc.font, line, x + 4, y + 3, profit >= 0 ? 0xFF55FF55 : 0xFFFF5555);
    }
}
