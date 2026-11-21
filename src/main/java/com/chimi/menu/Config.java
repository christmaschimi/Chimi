package com.chimi.menu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Saved to config/chimismenu.json. Everything the Customize tab changes lives here. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("chimismenu.json");
    public static Config I = new Config();

    public int accent = 0xFFFFB347;
    public int panelAlpha = 200;
    public boolean hudEnabled = true;
    public int hudCorner = 0; // 0 TL, 1 TR, 2 BL, 3 BR
    public String skyPreset = "Default";
    public String apiKey = "";      // DonutSMP API key (get it with /api in game)
    public String apiServerMatch = "donut"; // servers whose address contains this use the DonutSMP API
    // Used on every other server (e.g. Prism SMP): reads your balance from chat when /bal is shown.
    // Group 1 = number, group 2 = optional k/m/b suffix. Edit in config/chimismenu.json if the wording differs.
    public String balanceRegex = "(?i)^\\s*(?:your\\s+)?(?:balance|bal|money)\\s*(?:is|:|-)?\\s*\\$?([\\d,]*\\.?\\d+)\\s*([kKmMbB])?";
    public boolean bgEnabled = true;
    public String bgImage = "";        // file name inside config/chimismenu/backgrounds
    public int bgDim = 80;             // 0-255 darkness over the image
    public boolean bgAllContainers = false; // false = survival inventory only
    public boolean useApi = false; // optional DonutSMP API; off by default (sidebar + /bal are used instead)
    // Reads the balance line shown on the right-side scoreboard. Group 1 = number, group 2 = optional k/m/b.
    public String sidebarRegex = "(?i)(?:money|balance|bal|cash|coins?)[^\\d$]{0,12}\\$?\\s*([\\d,]*\\.?\\d+)\\s*([kKmMbB])?";
    public int pollSeconds = 20;    // balance refresh rate (API limit is 250 requests/min)

    public static void load() {
        try {
            if (Files.exists(FILE)) I = GSON.fromJson(Files.readString(FILE), Config.class);
        } catch (Exception e) {
            I = new Config();
        }
        if (I == null) I = new Config();
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(I));
        } catch (Exception ignored) {}
    }
}
