package com.chimi.menu;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** Imports images from your PC (png/jpg/gif/bmp), stores them as PNG, and draws one behind your inventory. */
public final class BackgroundManager {
    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("chimismenu").resolve("backgrounds");
    private static final Identifier TEX = Identifier.fromNamespaceAndPath(ChimiMenuClient.ID, "custom_bg");

    private static boolean loaded;
    private static String loadedName = "";
    private static int imgW, imgH;

    private BackgroundManager() {}

    public static List<String> list() {
        List<String> names = new ArrayList<>();
        try {
            Files.createDirectories(DIR);
            try (Stream<Path> s = Files.list(DIR)) {
                s.map(p -> p.getFileName().toString())
                        .filter(n -> n.toLowerCase().endsWith(".png"))
                        .forEach(names::add);
            }
        } catch (IOException ignored) {}
        Collections.sort(names);
        return names;
    }

    /** Copies any readable image into the backgrounds folder as PNG and selects it. */
    public static String importImage(Path src) throws IOException {
        BufferedImage img = ImageIO.read(src.toFile());
        if (img == null) throw new IOException("Not a supported image (use PNG, JPG, GIF or BMP)");
        Files.createDirectories(DIR);
        String base = src.getFileName().toString().replaceAll("\\.[^.]+$", "").replaceAll("[^A-Za-z0-9_-]", "_");
        if (base.isEmpty()) base = "background";
        String name = base + ".png";
        int n = 2;
        while (Files.exists(DIR.resolve(name))) name = base + "_" + n++ + ".png";
        ImageIO.write(img, "png", DIR.resolve(name).toFile());
        select(name);
        return name;
    }

    public static void select(String name) {
        Config.I.bgImage = name == null ? "" : name;
        Config.save();
        loaded = false; // reloaded on next draw
    }

    private static void ensureLoaded() {
        String want = Config.I.bgImage == null ? "" : Config.I.bgImage;
        if (loaded && want.equals(loadedName)) return;
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().release(TEX);
        loaded = false;
        loadedName = want;
        if (want.isEmpty()) return;
        Path file = DIR.resolve(want);
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage ni = NativeImage.read(in);
            imgW = ni.getWidth();
            imgH = ni.getHeight();
            mc.getTextureManager().register(TEX, new DynamicTexture(() -> "chimismenu_bg", ni));
            loaded = true;
        } catch (Exception e) {
            loaded = false;
        }
    }

    /** Draws the image to fill the screen (cropped, not stretched), then a dark overlay so slots stay readable. */
    public static void draw(GuiGraphicsExtractor g, int sw, int sh) {
        if (!Config.I.bgEnabled || Config.I.bgImage == null || Config.I.bgImage.isEmpty()) return;
        ensureLoaded();
        if (!loaded) return;
        float scale = Math.max((float) sw / imgW, (float) sh / imgH);
        int regionW = Math.min(imgW, Math.round(sw / scale));
        int regionH = Math.min(imgH, Math.round(sh / scale));
        float u = (imgW - regionW) / 2f;
        float v = (imgH - regionH) / 2f;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, 0, 0, u, v, sw, sh, regionW, regionH, imgW, imgH);
        if (Config.I.bgDim > 0) g.fill(0, 0, sw, sh, (Config.I.bgDim << 24));
    }
}
