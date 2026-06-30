package com.boxergame;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FightView extends JPanel {

    public static final int ANIM_NORMAL = 0, ANIM_PUNCH = 1, ANIM_BLOCK = 2,
                             ANIM_CHARGED = 3, ANIM_HURT = 4;

    private static final Color CLR_HP_GREEN = new Color(0x2ECC40);
    private static final Color CLR_HP_YELLOW = new Color(0xFFDC00);
    private static final Color CLR_HP_RED = new Color(0xFF4136);
    private static final Color CLR_HP_BG = new Color(0, 0, 0, 153);
    private static final Color CLR_BAR_FRAME = Color.WHITE;
    private static final int SPRITE_W = 160, SPRITE_H = 240;

    private BufferedImage mBackground;
    private BufferedImage mPlayerNormal, mPlayerPunch, mPlayerBlock, mPlayerCharge, mPlayerHurt;
    private BufferedImage mEnemyNormal,  mEnemyPunch,  mEnemyBlock,  mEnemyHurt;

    private float mPlayerHpPct = 1f, mEnemyHpPct = 1f;
    private int mPlayerPos = 2, mEnemyPos = 2;
    private int mPlayerAnim = 0, mEnemyAnim = 0;

    private String mFloatText = "";
    private float  mFloatAlpha = 0f, mFloatY = 0f;
    private Color  mFloatColor = Color.YELLOW;

    private float mShakeX = 0, mShakeY = 0;

    private float mFlashAlpha = 0f;
    private Color mFlashColor = Color.WHITE;

    private final List<Particle>     mParticles = new ArrayList<>();
    private final List<ImpactRing>   mImpactRings = new ArrayList<>();
    private final List<MotionStreak> mStreaks = new ArrayList<>();
    private final Random             mRnd = new Random();

    private Timer mEffectTimer;

    public FightView() {
        setPreferredSize(new Dimension(720, 960));
        setBackground(new Color(0x0d0d1a));
        setFocusable(true);
    }

    public void setArenaBackground(String name) {
        mBackground = Assets.getImage(name, 720, 960);
        repaint();
    }

    public void setPlayerSprites(String normal, String punch, String block,
                                  String charge, String hurt) {
        mPlayerNormal = Assets.getImage(normal, SPRITE_W, SPRITE_H);
        mPlayerPunch = Assets.getImage(punch,  SPRITE_W, SPRITE_H);
        mPlayerBlock = Assets.getImage(block,  SPRITE_W, SPRITE_H);
        mPlayerCharge = Assets.getImage(charge, SPRITE_W, SPRITE_H);
        mPlayerHurt = Assets.getImage(hurt,   SPRITE_W, SPRITE_H);
        repaint();
    }

    public void setEnemySprites(String normal, String punch, String block, String hurt) {
        mEnemyNormal = Assets.getImage(normal, SPRITE_W, SPRITE_H);
        mEnemyPunch = Assets.getImage(punch,  SPRITE_W, SPRITE_H);
        mEnemyBlock = Assets.getImage(block,  SPRITE_W, SPRITE_H);
        mEnemyHurt = Assets.getImage(hurt,   SPRITE_W, SPRITE_H);
        repaint();
    }

    public void updateFightState(float playerHpPct, float enemyHpPct,
                                  int playerPos, int enemyPos,
                                  int playerAnim, int enemyAnim) {
        mPlayerHpPct = playerHpPct;
        mEnemyHpPct = enemyHpPct;
        mPlayerPos = playerPos;
        mEnemyPos = enemyPos;
        mPlayerAnim = playerAnim;
        mEnemyAnim = enemyAnim;
        repaint();
    }

    public void showFloatText(String text, Color color) {
        mFloatText = text;
        mFloatAlpha = 1f;
        mFloatColor = color;
        mFloatY = getHeight() * 0.45f;
        startEffectLoop();
    }

    public void triggerImpact(float x, float y, boolean charged) {
        int count = charged ? 30 : 15;
        Color color = charged ? new Color(0xFFAA00) : Color.WHITE;
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            float speed = 200 + mRnd.nextFloat() * (charged ? 400 : 200);
            mParticles.add(new Particle(x, y, (float) Math.cos(angle) * speed,
                                        (float) Math.sin(angle) * speed, color,
                                        4 + mRnd.nextFloat() * 8));
        }
        mImpactRings.add(new ImpactRing(x, y, charged ? 150f : 80f));
        if (charged) mStreaks.add(new MotionStreak(x, y, 200f, color));
        mFlashAlpha = charged ? 0.4f : 0.2f;
        mFlashColor = charged ? new Color(0xFFAA00) : Color.WHITE;
        startEffectLoop();
    }

    public void triggerScreenShake(float intensity) {
        final float[] remaining = {intensity};
        Timer shakeTimer = new Timer(16, null);
        shakeTimer.addActionListener(e -> {
            remaining[0] = Math.max(0, remaining[0] - intensity * 0.08f);
            if (remaining[0] <= 0) {
                mShakeX = mShakeY = 0;
                ((Timer) e.getSource()).stop();
            } else {
                mShakeX = (mRnd.nextFloat() - 0.5f) * remaining[0] * 20f;
                mShakeY = (mRnd.nextFloat() - 0.5f) * remaining[0] * 20f;
            }
            repaint();
        });
        shakeTimer.start();
    }

    private void startEffectLoop() {
        if (mEffectTimer != null && mEffectTimer.isRunning()) return;
        mEffectTimer = new Timer(16, e -> {
            boolean any = false;
            float dt = 0.016f;
            for (int i = mParticles.size() - 1; i >= 0; i--) {
                Particle p = mParticles.get(i);
                p.update(dt);
                if (p.alpha <= 0) mParticles.remove(i); else any = true;
            }
            for (int i = mImpactRings.size() - 1; i >= 0; i--) {
                ImpactRing r = mImpactRings.get(i);
                r.update(dt);
                if (r.alpha <= 0) mImpactRings.remove(i); else any = true;
            }
            for (int i = mStreaks.size() - 1; i >= 0; i--) {
                MotionStreak s = mStreaks.get(i);
                s.update(dt);
                if (s.alpha <= 0) mStreaks.remove(i); else any = true;
            }
            if (mFlashAlpha > 0) { mFlashAlpha -= 0.05f; any = true; }
            if (mFloatAlpha > 0) { mFloatAlpha -= 0.04f; mFloatY -= 1.5f; any = true; }
            repaint();
            if (!any) { mEffectTimer.stop(); mEffectTimer = null; }
        });
        mEffectTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g2d) {
        super.paintComponent(g2d);
        Graphics2D g = (Graphics2D) g2d;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int W = getWidth(), H = getHeight();

        g.translate(mShakeX, mShakeY);

        if (mBackground != null) {
            g.drawImage(mBackground, 0, 0, W, H, null);
        } else {
            g.setColor(new Color(0x1a1a2e));
            g.fillRect(0, 0, W, H);
        }

        int floorY = (int) (H * 0.72f);
        g.setColor(new Color(255, 255, 255, 100));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(0, floorY, W, floorY);

        drawPositionStrip(g, W, H);

        float slotW = W / (float) GameConstants.POSITION_SLOTS;
        float eCentreX = slotW * (GameConstants.POSITION_SLOTS - 1 - mEnemyPos) + slotW / 2f;
        drawCharacter(g, selectEnemySprite(), (int) eCentreX, floorY - 10, SPRITE_W, SPRITE_H, true,
                      new Color(0xE74C3C));

        float pCentreX = slotW * mPlayerPos + slotW / 2f;
        drawCharacter(g, selectPlayerSprite(), (int) pCentreX, floorY - 10, SPRITE_W, SPRITE_H, false,
                      new Color(0x3498DB));

        for (MotionStreak s : mStreaks) s.draw(g);
        for (ImpactRing  r : mImpactRings) r.draw(g);
        for (Particle    p : mParticles) p.draw(g);

        if (mFlashAlpha > 0) {
            g.setColor(withAlpha(mFlashColor, mFlashAlpha));
            g.fillRect(0, 0, W, H);
        }

        g.translate(-mShakeX, -mShakeY);

        drawHpBars(g, W, H);

        if (mFloatAlpha > 0) {
            g.setFont(new Font("SansSerif", Font.BOLD, 46));
            g.setColor(withAlpha(mFloatColor, mFloatAlpha));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(mFloatText, (W - fm.stringWidth(mFloatText)) / 2, (int) mFloatY);
        }
    }

    private void drawCharacter(Graphics2D g, BufferedImage bmp, int cx, int floorY,
                                int w, int h, boolean flipH, Color fallback) {
        if (bmp == null) {
            g.setColor(fallback);
            g.fillRoundRect(cx - w / 2, floorY - h, w, h, 18, 18);
            return;
        }
        if (flipH) {
            g.drawImage(bmp, cx + w / 2, floorY - h, -w, h, null);
        } else {
            g.drawImage(bmp, cx - w / 2, floorY - h, w, h, null);
        }
    }

    private void drawPositionStrip(Graphics2D g, int W, int H) {
        float slotW = W / (float) GameConstants.POSITION_SLOTS;
        int sy = (int) (H * 0.76f), sh = 12;
        for (int i = 0; i < GameConstants.POSITION_SLOTS; i++) {
            int x = (int) (i * slotW);
            g.setColor(new Color(255, 255, 255, 68));
            g.fillRoundRect(x + 4, sy, (int) slotW - 8, sh, 6, 6);
            if (i == mPlayerPos) {
                g.setColor(new Color(0x3498DB));
                g.fillRoundRect(x + 4, sy, (int) slotW - 8, sh, 6, 6);
            }
            if (i == mEnemyPos) {
                g.setColor(new Color(0xE74C3C));
                g.fillRoundRect(x + 4, sy + 14, (int) slotW - 8, sh, 6, 6);
            }
        }
    }

    private void drawHpBars(Graphics2D g, int W, int H) {
        float barH = 22f, barW = W * 0.4f, barY = H * 0.04f, margin = W * 0.03f;
        drawHpBar(g, (int) margin, (int) barY, (int) barW, (int) barH, mPlayerHpPct, false);
        drawHpBar(g, (int) (W - margin - barW), (int) barY, (int) barW, (int) barH, mEnemyHpPct, true);
    }

    private void drawHpBar(Graphics2D g, int x, int y, int w, int h, float pct, boolean reverse) {
        g.setColor(CLR_HP_BG);
        g.fillRoundRect(x, y, w, h, h, h);
        int fillW = (int) (w * Math.max(0, Math.min(1f, pct)));
        Color fill = pct > 0.5f ? CLR_HP_GREEN : pct > 0.25f ? CLR_HP_YELLOW : CLR_HP_RED;
        g.setColor(fill);
        if (!reverse) g.fillRoundRect(x, y, fillW, h, h, h);
        else          g.fillRoundRect(x + w - fillW, y, fillW, h, h, h);
        g.setColor(CLR_BAR_FRAME);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, h, h);
    }

    private BufferedImage selectPlayerSprite() {
        switch (mPlayerAnim) {
            case ANIM_PUNCH:   return mPlayerPunch  != null ? mPlayerPunch  : mPlayerNormal;
            case ANIM_BLOCK:   return mPlayerBlock  != null ? mPlayerBlock  : mPlayerNormal;
            case ANIM_CHARGED: return mPlayerCharge != null ? mPlayerCharge : mPlayerNormal;
            case ANIM_HURT:    return mPlayerHurt   != null ? mPlayerHurt   : mPlayerNormal;
            default:           return mPlayerNormal;
        }
    }

    private BufferedImage selectEnemySprite() {
        switch (mEnemyAnim) {
            case ANIM_PUNCH:
            case ANIM_CHARGED: return mEnemyPunch != null ? mEnemyPunch : mEnemyNormal;
            case ANIM_BLOCK:   return mEnemyBlock != null ? mEnemyBlock : mEnemyNormal;
            case ANIM_HURT:    return mEnemyHurt  != null ? mEnemyHurt  : mEnemyNormal;
            default:           return mEnemyNormal;
        }
    }

    private static Color withAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 255));
    }

    private static class Particle {
        float x, y, vx, vy, alpha = 1f, size;
        Color color;
        Particle(float x, float y, float vx, float vy, Color c, float size) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.color = c; this.size = size;
        }
        void update(float dt) { x += vx * dt; y += vy * dt; vy += 500 * dt; alpha -= 2f * dt; size -= 10f * dt; }
        void draw(Graphics2D g) {
            if (alpha <= 0 || size <= 0) return;
            g.setColor(withAlpha(color, alpha));
            int s = (int) size;
            g.fillOval((int) x - s / 2, (int) y - s / 2, s, s);
        }
    }

    private static class ImpactRing {
        float x, y, radius, maxRadius, alpha = 1f;
        ImpactRing(float x, float y, float maxRadius) { this.x = x; this.y = y; this.maxRadius = maxRadius; this.radius = 10f; }
        void update(float dt) { radius += maxRadius * 4f * dt; alpha -= 3f * dt; }
        void draw(Graphics2D g) {
            if (alpha <= 0) return;
            g.setColor(withAlpha(Color.WHITE, alpha));
            g.setStroke(new BasicStroke(6f * alpha));
            int r = (int) radius;
            g.drawOval((int) x - r, (int) y - r, r * 2, r * 2);
        }
    }

    private static class MotionStreak {
        float x, y, length, alpha = 1f;
        Color color;
        MotionStreak(float x, float y, float length, Color c) { this.x = x; this.y = y; this.length = length; this.color = c; }
        void update(float dt) { length -= 400f * dt; alpha -= 2f * dt; }
        void draw(Graphics2D g) {
            if (alpha <= 0 || length <= 0) return;
            g.setColor(withAlpha(color, alpha * 0.5f));
            g.setStroke(new BasicStroke(8f));
            g.drawLine((int) (x - length), (int) y, (int) x, (int) y);
        }
    }
}
