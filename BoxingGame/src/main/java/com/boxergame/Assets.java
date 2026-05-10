package com.boxergame;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Desktop replacement for Android's R.drawable / R.raw resource system.
 * PNGs are loaded from /drawable/<name>.png bundled in the JAR.
 * WAVs are loaded from /raw/<name>.wav bundled in the JAR.
 * Vector XML drawables (items, endings) are rendered procedurally.
 */
public final class Assets {

    private static final Map<String, BufferedImage> imageCache = new HashMap<>();

    private Assets() {}

    // ── Images ────────────────────────────────────────────────────────────────

    /** Returns a scaled image, or a generated fallback if the PNG is missing. */
    public static BufferedImage getImage(String name, int w, int h) {
        if (name == null) return null;
        String key = name + "_" + w + "_" + h;
        return imageCache.computeIfAbsent(key, k -> loadOrGenerate(name, w, h));
    }

    private static BufferedImage loadOrGenerate(String name, int w, int h) {
        BufferedImage src = loadPng(name);
        if (src != null) return scaleTo(src, w, h);
        // Fall back to procedurally generated image
        return generateFallback(name, w, h);
    }

    private static BufferedImage loadPng(String name) {
        String path = "/drawable/" + name + ".png";
        URL url = Assets.class.getResource(path);
        if (url == null) return null;
        try { return ImageIO.read(url); } catch (IOException e) { return null; }
    }

    private static BufferedImage scaleTo(BufferedImage src, int w, int h) {
        if (src.getWidth() == w && src.getHeight() == h) return src;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /**
     * Generates a simple colored icon for XML vector drawables we can't load directly.
     * Covers: item_gloves, item_shoes, item_armor, item_muscle,
     *         ending_legend, ending_forgotten, ending_rich, ending_beloved,
     *         bg_* arena backgrounds.
     */
    private static BufferedImage generateFallback(String name, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (name) {
            case "item_gloves":
                drawRoundedRect(g, w, h, new Color(0xE53935), new Color(0xFFCDD2), "🥊");
                break;
            case "item_shoes":
                drawRoundedRect(g, w, h, new Color(0x1565C0), new Color(0xBBDEFB), "👟");
                break;
            case "item_armor":
                drawRoundedRect(g, w, h, new Color(0x37474F), new Color(0xB0BEC5), "🛡");
                break;
            case "item_muscle":
                drawRoundedRect(g, w, h, new Color(0xFF8F00), new Color(0xFFECB3), "💪");
                break;
            case "ending_legend":
                drawEndingCard(g, w, h, new Color(0xFFD700), "🌟 LEGEND");
                break;
            case "ending_forgotten":
                drawEndingCard(g, w, h, new Color(0x607D8B), "😞 FORGOTTEN");
                break;
            case "ending_rich":
                drawEndingCard(g, w, h, new Color(0x4CAF50), "💰 RICH");
                break;
            case "ending_beloved":
                drawEndingCard(g, w, h, new Color(0xE91E63), "⭐ BELOVED");
                break;
            default:
                // Arena backgrounds — solid dark colours
                g.setColor(bgColor(name));
                g.fillRect(0, 0, w, h);
                // Simple floor/spotlight gradient
                GradientPaint gp = new GradientPaint(w/2f, 0, new Color(0,0,0,0),
                                                     w/2f, h, new Color(255,255,255,30));
                g.setPaint(gp);
                g.fillRect(0, 0, w, h);
                break;
        }
        g.dispose();
        return img;
    }

    private static void drawRoundedRect(Graphics2D g, int w, int h, Color bg, Color fg, String emoji) {
        g.setColor(bg);
        g.fillRoundRect(2, 2, w-4, h-4, 12, 12);
        g.setColor(fg);
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, (int)(w * 0.55f)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(emoji, (w - fm.stringWidth(emoji))/2,
                     (h + fm.getAscent() - fm.getDescent())/2);
    }

    private static void drawEndingCard(Graphics2D g, int w, int h, Color accent, String label) {
        g.setColor(new Color(0x1A1A2E));
        g.fillRect(0, 0, w, h);
        // Outer ring
        g.setColor(accent);
        g.setStroke(new BasicStroke(6f));
        g.drawOval(10, 10, w-20, h-20);
        // Label
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, w/12)));
        g.setColor(accent);
        FontMetrics fm = g.getFontMetrics();
        String[] parts = label.split(" ", 2);
        g.drawString(parts[0], (w - fm.stringWidth(parts[0]))/2, h/2 - 6);
        if (parts.length > 1)
            g.drawString(parts[1], (w - fm.stringWidth(parts[1]))/2, h/2 + fm.getHeight());
    }

    // ── Background colour lookup ───────────────────────────────────────────────

    public static Color bgColor(String name) {
        if (name == null) return new Color(0x0d0d1a);
        switch (name) {
            case "bg_gym":                return new Color(0x2d1b0e);
            case "bg_arena_small":        return new Color(0x1a0a1a);
            case "bg_arena_mid":          return new Color(0x0d0d2e);
            case "bg_arena_championship": return new Color(0x1a0a00);
            case "bg_underground":        return new Color(0x050510);
            case "bg_lobby":              return new Color(0x0d0d1a);
            default:                      return new Color(0x0d0d1a);
        }
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    /** Open a Clip for a sound file. Returns null if not found or audio unavailable. */
    public static Clip openClip(String name) {
        String path = "/raw/" + name + ".wav";
        InputStream is = Assets.class.getResourceAsStream(path);
        if (is == null) return null;
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(is);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }
}
