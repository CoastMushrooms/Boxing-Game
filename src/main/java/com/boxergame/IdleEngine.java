package com.boxergame;

import javax.swing.SwingUtilities;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Muscle Memory — three modes:
 *
 * 1. LOBBY TICK  — drips money every 2s while app is open in lobby.
 *
 * 2. FIGHT PASSIVE — onPlayerPunch/onPlayerChargedPunch add a bonus on top of
 *    the player's own earnings each time they punch. Player stays in control.
 *
 * 3. OFFLINE SIMULATION — on next launch, calculates money earned while away
 *    by simulating punches at a conservative rate (capped at 8 hours).
 *
 * 4. AUTO-FIGHT (when player is in a live fight and goes idle) — FightActivity
 *    calls startAutoFight(). The engine will throw punches, blocks, and dodges
 *    automatically at a reduced rate using the fight engine callbacks. It pauses
 *    as soon as the player makes any real input (stopAutoFight).
 */
public class IdleEngine {

    // ── Constants ─────────────────────────────────────────────────────────────
    public  static final long  IDLE_TICK_MS             = 2000L;
    private static final float IDLE_TICK_EARN            = 5f;
    public  static final float FIGHT_PASSIVE_BONUS       = 2f;
    private static final long  OFFLINE_PUNCH_INTERVAL_MS = 4000L;
    private static final float OFFLINE_PUNCH_EARN        = GameConstants.BASE_PUNCH_EARN * 0.6f;
    private static final float MAX_OFFLINE_HOURS         = 8f;

    // Auto-fight: one action every X ms (slower than player to keep it fair)
    private static final long  AUTO_FIGHT_INTERVAL_MS    = 1800L;

    // ── State ─────────────────────────────────────────────────────────────────
    private final GameState          mState  = GameState.get();
    // mMain replaced by SwingUtilities.invokeLater
    private final AtomicBoolean      mActive = new AtomicBoolean(false);
    private final AtomicBoolean      mAutoFighting = new AtomicBoolean(false);
    private final ScheduledExecutorService mExec =
            Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> mLobbyTask;
    private ScheduledFuture<?> mAutoTask;

    private IdleCallback       mCallback;
    private AutoFightCallback  mAutoCallback;
    private final Random       mRng = new Random();

    public interface IdleCallback {
        void onIdleTick(float newMoney, float earned);
    }

    public interface AutoFightCallback {
        void doAutoAction(int action); // MOVE_PUNCH / MOVE_BLOCK / MOVE_DODGE_L / MOVE_DODGE_R / MOVE_CHARGED
    }

    // ── Lobby passive tick ────────────────────────────────────────────────────

    public void start(IdleCallback cb) {
        if (!mState.muscleMemoryUnlocked.get()) return;
        if (mActive.getAndSet(true)) return;
        mCallback = cb;
        mLobbyTask = mExec.scheduleAtFixedRate(
                this::tick, IDLE_TICK_MS, IDLE_TICK_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        mActive.set(false);
        cancelTask(mLobbyTask);
        mLobbyTask = null;
    }

    public boolean isActive() { return mActive.get(); }

    public void destroy() {
        stop();
        stopAutoFight();
        mExec.shutdownNow();
    }

    private void tick() {
        if (!mActive.get() || !mState.muscleMemoryUnlocked.get()) { stop(); return; }
        float earned = IDLE_TICK_EARN;
        mState.addMoneyIdle(earned);
        final float newMoney = mState.getMoney();
        final float display  = earned * GameConstants.IDLE_EARN_MULT;
        if (mCallback != null) SwingUtilities.invokeLater(() -> mCallback.onIdleTick(newMoney, display));
    }

    // ── Fight passive bonus ───────────────────────────────────────────────────

    public float onPlayerPunch() {
        if (!mState.muscleMemoryUnlocked.get()) return 0f;
        float bonus = FIGHT_PASSIVE_BONUS * mState.getEarnMult();
        mState.addMoney(bonus);
        return bonus;
    }

    public float onPlayerChargedPunch() {
        if (!mState.muscleMemoryUnlocked.get()) return 0f;
        float bonus = FIGHT_PASSIVE_BONUS * 2.5f * mState.getEarnMult();
        mState.addMoney(bonus);
        return bonus;
    }

    // ── Auto-fight (fires when player is idle during a live fight) ────────────

    /**
     * Start auto-fighting. FightActivity calls this after a period of player
     * inactivity. The auto-fighter throws a mix of punches, blocks, and dodges.
     * It is NOT a replacement — it yields instantly when the player acts again.
     */
    public void startAutoFight(AutoFightCallback cb) {
        if (!mState.muscleMemoryUnlocked.get()) return;
        if (mAutoFighting.getAndSet(true)) return;
        mAutoCallback = cb;
        mAutoTask = mExec.scheduleAtFixedRate(
                this::autoFightTick, AUTO_FIGHT_INTERVAL_MS, AUTO_FIGHT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /** Call this immediately when the player makes any real input. */
    public void stopAutoFight() {
        mAutoFighting.set(false);
        cancelTask(mAutoTask);
        mAutoTask = null;
    }

    public boolean isAutoFighting() { return mAutoFighting.get(); }

    private void autoFightTick() {
        if (!mAutoFighting.get() || !mState.muscleMemoryUnlocked.get()) {
            stopAutoFight(); return;
        }
        if (mAutoCallback == null) return;

        // Weighted random action: mostly punch, sometimes block or dodge
        int action;
        float r = mRng.nextFloat();
        if (r < 0.55f)      action = FightEngine.MOVE_PUNCH;
        else if (r < 0.70f) action = FightEngine.MOVE_BLOCK;
        else if (r < 0.80f) action = FightEngine.MOVE_CHARGED;
        else if (r < 0.90f) action = FightEngine.MOVE_DODGE_L;
        else                action = FightEngine.MOVE_DODGE_R;

        final int finalAction = action;
        SwingUtilities.invokeLater(() -> {
            if (mAutoFighting.get() && mAutoCallback != null)
                mAutoCallback.doAutoAction(finalAction);
        });
    }

    // ── Offline simulation ────────────────────────────────────────────────────

    public OfflineResult calculateOfflineEarnings(long lastOnlineMs) {
        if (!mState.muscleMemoryUnlocked.get()) return null;
        long now     = System.currentTimeMillis();
        long elapsed = now - lastOnlineMs;
        if (elapsed < OFFLINE_PUNCH_INTERVAL_MS * 3) return null;

        long maxMs   = (long)(MAX_OFFLINE_HOURS * 3_600_000L);
        elapsed      = Math.min(elapsed, maxMs);

        long  punches    = elapsed / OFFLINE_PUNCH_INTERVAL_MS;
        float earnEach   = OFFLINE_PUNCH_EARN * mState.getEarnMult();
        float total      = punches * earnEach;

        mState.addMoneyIdle(total / GameConstants.IDLE_EARN_MULT);
        mState.saveState();

        return new OfflineResult(punches, total, elapsed / 60_000L);
    }

    public static class OfflineResult {
        public final long  punchesSimulated;
        public final float moneyEarned;
        public final long  minutesAway;
        OfflineResult(long p, float m, long min) {
            punchesSimulated = p; moneyEarned = m; minutesAway = min;
        }
        public String summary() {
            String time = minutesAway >= 60
                    ? (minutesAway / 60) + "h " + (minutesAway % 60) + "m"
                    : minutesAway + "m";
            return "💪 Muscle Memory kept fighting while you were away!\n\n"
                 + "Time away: " + time + "\n"
                 + "Punches thrown: " + punchesSimulated + "\n"
                 + "Money earned:  $" + (int) moneyEarned;
        }
    }

    private void cancelTask(ScheduledFuture<?> t) {
        if (t != null && !t.isCancelled()) t.cancel(false);
    }
}
