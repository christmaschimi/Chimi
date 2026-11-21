package com.chimi.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChimiScreen extends Screen {
    private static final String[] TABS = {"Auction", "Profit", "Sky", "Inventory", "Customize"};
    private static final int[] ACCENTS = {0xFFFFB347, 0xFF5CD6FF, 0xFFB36BFF, 0xFF7CFF6B, 0xFFFF5C8A};
    private static final int[] ALPHAS = {120, 160, 200, 235};
    private static final String[] CORNERS = {"Top left", "Top right", "Bottom left", "Bottom right"};
    private static final String[] SKIES = {"Default", "Aurora", "Galaxy", "Sunset", "Neon", "Blood Moon"};
    private static final int W = 340, H = 240;

    private static int tab = 0;
    private static int selectedSlot = -1;
    private static String durationText = "300";
    private static String notice = "";

    private final List<int[]> cells = new ArrayList<>(); // {slot, x, y}
    private int px, py;

    public ChimiScreen() {
        super(Component.literal("Chimi's Menu"));
    }

    private static Component t(String s) { return Component.literal(s); }

    private void add(String label, int x, int y, int w, Runnable r) {
        addRenderableWidget(Button.builder(t(label), b -> r.run()).bounds(x, y, w, 18).build());
    }

    private static int parseSeconds() {
        try {
            int s = Integer.parseInt(durationText.trim());
            return Math.max(10, Math.min(86400, s));
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    private static String fmtTime(int s) {
        return (s >= 3600 ? (s / 3600) + "h " : "") + ((s % 3600) / 60) + "m " + (s % 60) + "s";
    }

    @Override
    protected void init() {
        cells.clear();
        px = (width - W) / 2;
        py = (height - H) / 2;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            add(TABS[i], px + 8 + i * 66, py + 24, 64, () -> { tab = idx; notice = ""; rebuildWidgets(); });
        }
        int y = py + 54;
        switch (tab) {
            case 0 -> buildAuction();
            case 1 -> {
                add(ChimiMenuClient.session.isActive() ? "Stop session" : "Start session", px + 8, y, 100, () -> {
                    if (ChimiMenuClient.session.isActive()) ChimiMenuClient.session.stop(); else ChimiMenuClient.startSession();
                    rebuildWidgets();
                });
                add("HUD: " + (Config.I.hudEnabled ? "On" : "Off"), px + 114, y, 70, () -> { Config.I.hudEnabled = !Config.I.hudEnabled; Config.save(); rebuildWidgets(); });
                add("Send /bal", px + 190, y, 80, () -> BalanceTracker.refresh(minecraft));
            }
            case 2 -> {
                for (int i = 0; i < SKIES.length; i++) {
                    final String name = SKIES[i];
                    String label = (name.equals(Config.I.skyPreset) ? "> " : "") + name;
                    add(label, px + 8 + (i % 3) * 108, y + (i / 3) * 24, 104, () -> { Config.I.skyPreset = name; Config.save(); rebuildWidgets(); });
                }
            }
            case 3 -> buildInventoryTab(y);
            case 4 -> {
                add("Accent color", px + 8, y, 110, () -> {
                    int i = 0;
                    for (int k = 0; k < ACCENTS.length; k++) if (ACCENTS[k] == Config.I.accent) i = k;
                    Config.I.accent = ACCENTS[(i + 1) % ACCENTS.length];
                    Config.save();
                });
                add("Panel opacity", px + 124, y, 110, () -> {
                    int i = 0;
                    for (int k = 0; k < ALPHAS.length; k++) if (ALPHAS[k] == Config.I.panelAlpha) i = k;
                    Config.I.panelAlpha = ALPHAS[(i + 1) % ALPHAS.length];
                    Config.save();
                });
                add("HUD: " + CORNERS[Config.I.hudCorner], px + 8, y + 24, 110, () -> { Config.I.hudCorner = (Config.I.hudCorner + 1) % 4; Config.save(); rebuildWidgets(); });
            }
        }
    }

    private void importFile(java.nio.file.Path p) {
        try {
            String name = BackgroundManager.importImage(p);
            notice = "Added " + name;
        } catch (Exception e) {
            notice = "Couldn't import: " + e.getMessage();
        }
        rebuildWidgets();
    }

    private void cycleBackground(int dir) {
        List<String> names = BackgroundManager.list();
        if (names.isEmpty()) return;
        int i = names.indexOf(Config.I.bgImage);
        i = i < 0 ? 0 : Math.floorMod(i + dir, names.size());
        BackgroundManager.select(names.get(i));
        rebuildWidgets();
    }

    private void buildInventoryTab(int y) {
        add("Add image", px + 8, y, 90, () -> {
            try {
                FilePicker.pickImage().ifPresent(this::importFile);
            } catch (Throwable t) {
                notice = "File picker unavailable. Drag an image onto this window.";
            }
        });
        add("Clear", px + 104, y, 60, () -> { BackgroundManager.select(""); rebuildWidgets(); });
        add("Enabled: " + (Config.I.bgEnabled ? "On" : "Off"), px + 170, y, 90, () -> { Config.I.bgEnabled = !Config.I.bgEnabled; Config.save(); rebuildWidgets(); });
        add("< Prev", px + 8, y + 24, 60, () -> cycleBackground(-1));
        add("Next >", px + 74, y + 24, 60, () -> cycleBackground(1));
        add("Darkness: " + (Config.I.bgDim * 100 / 255) + "%", px + 8, y + 48, 110, () -> {
            Config.I.bgDim = Config.I.bgDim >= 200 ? 0 : Config.I.bgDim + 40;
            Config.save();
            rebuildWidgets();
        });
        add(Config.I.bgAllContainers ? "Scope: All containers" : "Scope: Inventory only", px + 124, y + 48, 130, () -> {
            Config.I.bgAllContainers = !Config.I.bgAllContainers;
            Config.save();
            rebuildWidgets();
        });
    }

    private void drawInventoryTab(GuiGraphicsExtractor g) {
        g.text(font, "Current: " + (Config.I.bgImage.isEmpty() ? "none" : Config.I.bgImage), px + 140, py + 82, 0xFFFFFFFF);
        g.text(font, "Saved images: " + BackgroundManager.list().size(), px + 8, py + 134, 0xFFAAAAAA);
        g.text(font, "Tip: you can also drag an image file onto this window.", px + 8, py + 148, 0xFFAAAAAA);
        g.text(font, "Reopen your inventory (E) to see it. Crop is automatic.", px + 8, py + 162, 0xFFAAAAAA);
        if (!notice.isEmpty()) g.text(font, notice, px + 8, py + 180, 0xFFFFD166);
    }

    @Override
    public void onFilesDrop(List<java.nio.file.Path> paths) {
        if (tab == 3 && !paths.isEmpty()) importFile(paths.get(0));
    }

    private void buildAuction() {
        // Inventory grid: click a slot to choose the item to auction.
        int gx = px + 8, gy = py + 62;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 9; c++) {
                final int slot = r < 3 ? 9 + r * 9 + c : c;
                int x = gx + c * 18, y = gy + r * 18 + (r == 3 ? 3 : 0);
                cells.add(new int[]{slot, x, y});
                addRenderableWidget(Button.builder(Component.empty(), b -> { selectedSlot = slot; notice = ""; })
                        .bounds(x, y, 18, 18).build());
            }
        }
        // Duration: type any number of seconds, or use a preset.
        int rx = px + 186;
        EditBox box = new EditBox(font, rx, py + 62, 80, 18, t("Seconds"));
        box.setValue(durationText);
        box.setResponder(s -> durationText = s);
        addRenderableWidget(box);
        int[] presets = {60, 300, 600, 1800};
        String[] names = {"1m", "5m", "10m", "30m"};
        for (int i = 0; i < presets.length; i++) {
            final int secs = presets[i];
            add(names[i], rx + i * 37, py + 84, 34, () -> { durationText = String.valueOf(secs); rebuildWidgets(); });
        }
        add("Start", rx, py + 108, 70, this::startAuction);
        add("End now", rx + 76, py + 108, 70, () -> { ChimiMenuClient.auction = null; ChimiMenuClient.auctionStack = ItemStack.EMPTY; });
    }

    private void startAuction() {
        if (minecraft == null || minecraft.player == null || selectedSlot < 0) { notice = "Pick an item first"; return; }
        ItemStack s = minecraft.player.getInventory().getItem(selectedSlot);
        if (s.isEmpty()) { notice = "That slot is empty"; return; }
        ChimiMenuClient.auctionStack = s.copy();
        ChimiMenuClient.auction = new EconomyCore.Auction(s.getCount() + "x " + s.getHoverName().getString(), parseSeconds());
        notice = "";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int accent = Config.I.accent;
        g.fill(px, py, px + W, py + H, (Config.I.panelAlpha << 24) | 0x101018);
        g.fill(px, py, px + W, py + 2, accent);
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.text(font, "Chimi's Menu", px + 8, py + 8, accent);

        switch (tab) {
            case 0 -> drawAuction(g, accent);
            case 1 -> drawProfit(g);
            case 2 -> g.text(font, "Selected: " + Config.I.skyPreset, px + 8, py + 130, 0xFFFFFFFF);
            case 3 -> drawInventoryTab(g);
            case 4 -> g.text(font, "Changes save automatically.", px + 8, py + 130, 0xFFAAAAAA);
        }
    }

    private void drawAuction(GuiGraphicsExtractor g, int accent) {
        g.text(font, "1. Pick the item", px + 8, py + 50, 0xFFAAAAAA);
        g.text(font, "2. Duration (seconds)", px + 186, py + 50, 0xFFAAAAAA);
        g.text(font, "= " + fmtTime(parseSeconds()), px + 272, py + 67, 0xFFFFFFFF);

        if (minecraft != null && minecraft.player != null) {
            for (int[] c : cells) {
                ItemStack st = minecraft.player.getInventory().getItem(c[0]);
                if (!st.isEmpty()) {
                    g.item(st, c[1] + 1, c[2] + 1);
                    g.itemDecorations(font, st, c[1] + 1, c[2] + 1);
                }
                if (c[0] == selectedSlot) {
                    g.fill(c[1], c[2], c[1] + 18, c[2] + 1, accent);
                    g.fill(c[1], c[2] + 17, c[1] + 18, c[2] + 18, accent);
                    g.fill(c[1], c[2], c[1] + 1, c[2] + 18, accent);
                    g.fill(c[1] + 17, c[2], c[1] + 18, c[2] + 18, accent);
                }
            }
            String sel = "Item: none";
            if (selectedSlot >= 0) {
                ItemStack st = minecraft.player.getInventory().getItem(selectedSlot);
                if (!st.isEmpty()) sel = "Item: " + st.getCount() + "x " + st.getHoverName().getString();
            }
            g.text(font, sel, px + 8, py + 142, 0xFFFFFFFF);
        }
        if (!notice.isEmpty()) g.text(font, notice, px + 186, py + 132, 0xFFFF5555);

        var a = ChimiMenuClient.auction;
        int y = py + 160;
        if (a == null) {
            g.text(font, "No auction running. Players pay you to bid.", px + 8, y, 0xFFAAAAAA);
            return;
        }
        if (!ChimiMenuClient.auctionStack.isEmpty()) g.item(ChimiMenuClient.auctionStack, px + 8, y - 4);
        g.text(font, a.prize + " - " + (a.isOver() ? "Finished" : fmtTime((int) a.secondsLeft()) + " left"), px + 30, y, 0xFFFFFFFF);
        List<Map.Entry<String, Double>> lb = a.leaderboard();
        for (int i = 0; i < Math.min(4, lb.size()); i++) {
            var e = lb.get(i);
            g.text(font, (i + 1) + ". " + e.getKey() + "  " + EconomyCore.format(e.getValue()), px + 8, y + 16 + i * 12, i == 0 ? accent : 0xFFDDDDDD);
        }
        if (a.isOver()) a.winner().ifPresent(w ->
                g.text(font, "Winner: " + w.getKey() + " (" + EconomyCore.format(w.getValue()) + ")", px + 200, y + 16, 0xFF55FF55));
    }

    private void drawProfit(GuiGraphicsExtractor g) {
        var s = ChimiMenuClient.session;
        int y = py + 54;
        g.text(font, "Reads the right-side scoreboard and your /bal replies.", px + 8, y + 28, 0xFFAAAAAA);
        int ty = py + 122;
        g.text(font, "Session: " + (s.isActive() ? "running" : "stopped") + "   " + BalanceTracker.status, px + 8, ty, 0xFFFFFFFF);
        boolean real = ChimiMenuClient.usingBalance();
        g.text(font, "Balance: " + (Double.isNaN(BalanceTracker.balance) ? "-" : EconomyCore.format(BalanceTracker.balance))
                + "   Start: " + (Double.isNaN(BalanceTracker.startBalance) ? "-" : EconomyCore.format(BalanceTracker.startBalance)), px + 8, ty + 14, 0xFFAAAAAA);
        double p = ChimiMenuClient.profit();
        g.text(font, "Profit: " + EconomyCore.format(p) + (real ? " (real balance)" : " (payments only)"), px + 8, ty + 28, p >= 0 ? 0xFF55FF55 : 0xFFFF5555);
        g.text(font, "Payments in: " + EconomyCore.format(s.received()) + "   out: " + EconomyCore.format(s.sent()), px + 8, ty + 42, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
