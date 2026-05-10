package com.boxergame;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Desktop SoundManager — replaces Android MediaPlayer / SoundPool.
 * Uses javax.sound.sampled (ships with all JDKs).
 */
public class SoundManager {

    private static final float SFX_GAIN   = -3f;   // dB
    private static final float MUSIC_GAIN = -12f;  // dB (quieter)

    private final Map<String, Clip> sfxCache = new HashMap<>();
    private Clip   musicClip;
    private String currentMusicName;

    public SoundManager() {}

    // ── SFX ──────────────────────────────────────────────────────────────────

    public void playSfx(String name) {
        try {
            Clip clip = sfxCache.get(name);
            if (clip == null) {
                clip = Assets.openClip(name);
                if (clip == null) return;
                sfxCache.put(name, clip);
            }
            setGain(clip, SFX_GAIN);
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception ignored) {}
    }

    // ── Music ─────────────────────────────────────────────────────────────────

    public void playMusic(String name, boolean loop) {
        if (name.equals(currentMusicName) && musicClip != null && musicClip.isRunning()) return;
        stopMusic();
        try {
            Clip clip = Assets.openClip(name);
            if (clip == null) return;
            setGain(clip, MUSIC_GAIN);
            if (loop) clip.loop(Clip.LOOP_CONTINUOUSLY);
            else      clip.start();
            musicClip        = clip;
            currentMusicName = name;
        } catch (Exception ignored) {}
    }

    public void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
            musicClip.close();
            musicClip        = null;
            currentMusicName = null;
        }
    }

    public void pauseMusic() {
        if (musicClip != null && musicClip.isRunning()) musicClip.stop();
    }

    public void resumeMusic() {
        if (musicClip != null && !musicClip.isRunning()) musicClip.start();
    }

    public void destroy() {
        stopMusic();
        sfxCache.values().forEach(c -> { try { c.close(); } catch (Exception ignored) {} });
        sfxCache.clear();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setGain(Clip clip, float db) {
        try {
            FloatControl fc = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            fc.setValue(Math.max(fc.getMinimum(), Math.min(fc.getMaximum(), db)));
        } catch (Exception ignored) {}
    }
}
