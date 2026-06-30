package com.boxergame;

import javax.swing.SwingUtilities;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class FightEngine {

    public static final int MOVE_PUNCH = 0, MOVE_BLOCK = 1,
                            MOVE_DODGE_L = 2, MOVE_DODGE_R = 3, MOVE_CHARGED = 4;

    private final GameState  mState = GameState.get();
    private final IdleEngine mIdle = new IdleEngine();
    private final ScheduledExecutorService mExec = Executors.newScheduledThreadPool(4);
    private final Random mRng = new Random();
    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    private EnemyData     mEnemy;
    private FightCallback mCallback;
    private ScheduledFuture<?> mAITask, mComboTask, mCornerTask, mFameTask;

    private final AtomicLong mLastPlayerPunchMs = new AtomicLong(0);
    private final AtomicLong mLastChargedPunchMs = new AtomicLong(0);

    private volatile long    mLastPlayerBlock = 0;
    private volatile int mPunchStreak = 0, mBlockStreak = 0;
    private volatile boolean mEnemyCharging = false;
    private volatile int mAIPattern = 0;
    private volatile boolean mFirstKillDone = false;

    private volatile boolean mEnemyIsBlocking = false;

    public interface FightCallback {
        void onPlayerDamaged(float hp, float dmg);
        void onEnemyDamaged(float hp, float dmg);
        void onPositionChanged(int pPos, int ePos);
        void onFameChanged(float fame, float delta);
        void onMoneyChanged(float money, float delta);
        void onComboPrompt(int[] moves);
        void onComboResult(boolean success);
        void onFightWon(float money, float fame);
        void onFightLost(float lost);
        void onFightEvent(String text);
        void onEnemyTelegraph(int attackType);
        void onImpactEffect(boolean hitPlayer, int moveType, float x, float y);
        void onScreenShake(float intensity);
        void onRoundReset();
        void onKnockback(boolean playerKnockedBack);
        void onEnemyBlocking(boolean blocking);  
    }

    public void startFight(EnemyData enemy, FightCallback cb) {
        if (mRunning.getAndSet(true)) stopFight();
        mEnemy = enemy;
        mCallback = cb;
        mFirstKillDone = false;
        mState.resetFight(enemy.maxHp);
        mPunchStreak = mBlockStreak = 0;
        mAIPattern = mRng.nextInt(3);
        mEnemyCharging = false;
        mEnemyIsBlocking = false;
        mLastPlayerPunchMs.set(0);
        mLastChargedPunchMs.set(0);
        scheduleAI();
        scheduleCombos();
        scheduleCornerPenalty();
        scheduleFameDrift();
        SwingUtilities.invokeLater(() -> cb.onFightEvent("FIGHT!"));
    }

    public void stopFight() {
        mRunning.set(false);
        cancelTask(mAITask);
        cancelTask(mComboTask);
        cancelTask(mCornerTask);
        cancelTask(mFameTask);
    }

    public void destroy() { stopFight(); mExec.shutdownNow(); }

    public void playerPunch() {
        if (!mRunning.get()) return;
        long now = System.currentTimeMillis();
        if (now - mLastPlayerPunchMs.get() < GameConstants.PUNCH_COOLDOWN_MS) return;
        mLastPlayerPunchMs.set(now);
        mExec.submit(() -> punch(false));
    }

    public void playerChargedPunch() {
        if (!mRunning.get()) return;
        long now = System.currentTimeMillis();
        if (now - mLastChargedPunchMs.get() < GameConstants.CHARGED_COOLDOWN_MS) return;
        mLastChargedPunchMs.set(now);
        mExec.submit(() -> punch(true));
    }

    public void playerBlock(boolean blocking) {
        if (!mRunning.get()) return;
        mState.setBlocking(blocking);
        if (blocking) {
            mLastPlayerBlock = System.currentTimeMillis();
            mBlockStreak++;
            mPunchStreak = 0;
            handleFame(mState.recordMove(MOVE_BLOCK));
        }
    }

    public void playerDodge(int dir) {
        if (!mRunning.get()) return;
        mExec.submit(() -> {
            mState.shiftPosition(dir > 0 ? -1 : 1);
            handleFame(mState.recordMove(dir > 0 ? MOVE_DODGE_R : MOVE_DODGE_L));
            notifyPos();
            reactToDodge();
        });
    }

    private void punch(boolean charged) {
        mPunchStreak++;
        mBlockStreak = 0;

        boolean enemyBlocked = mEnemyIsBlocking;
        float rawDmg = (charged ? GameConstants.CHARGED_DAMAGE_MULT : 1f) * GameConstants.BASE_PLAYER_DAMAGE;
        float dmg = enemyBlocked ? rawDmg * 0.15f : rawDmg;

        boolean dead = mState.damageEnemy(dmg);

        float earn = charged ? GameConstants.CHARGED_PUNCH_EARN : GameConstants.BASE_PUNCH_EARN;
        mState.addMoney(earn);

        float passiveBonus = charged ? mIdle.onPlayerChargedPunch() : mIdle.onPlayerPunch();

        int mv = charged ? MOVE_CHARGED : MOVE_PUNCH;
        float fame = handleFame(mState.recordMove(mv));
        if (charged) { mState.addFame(GameConstants.FAME_CHARGED_BONUS); fame += GameConstants.FAME_CHARGED_BONUS; }

        if (mState.comboActive.get()) {
            int r = mState.inputComboMove(mv);
            if (r == 2) comboWin();
            else if (r == 0) comboLose();
        }

        if (!enemyBlocked) stepTowardCorner(false);

        final float fDmg = dmg;
        final float fEarn = (earn * mState.getEarnMult()) + passiveBonus;
        final float fFame = fame;
        final int fMv = mv;
        final boolean fBlocked = enemyBlocked;

        SwingUtilities.invokeLater(() -> {
            mCallback.onImpactEffect(false, fMv, getEnemyScreenX(), getEnemyScreenY());
            mCallback.onScreenShake(fBlocked ? 0.1f : charged ? 0.8f : 0.3f);
            mCallback.onEnemyDamaged(mState.getEnemyHp(), fDmg);
            mCallback.onMoneyChanged(mState.getMoney(), fEarn);
            mCallback.onFameChanged(mState.getFame(), fFame);
            if (fBlocked) mCallback.onFightEvent("🛡 BLOCKED!");
            notifyPos();
        });

        reactToHit(charged, enemyBlocked);

        if (dead) handleEnemyDeath();
    }

    private void handleEnemyDeath() {
        if (!mFirstKillDone) {
            mFirstKillDone = true;
            float rewardFame = mEnemy.fameReward;
            float rewardMoney = mEnemy.moneyReward;
            mState.addFame(rewardFame);
            mState.addMoney(rewardMoney);
            mState.saveState();
            SaveManager.saveChampionshipProgress(mState, mEnemy);
            SwingUtilities.invokeLater(() -> {
                mCallback.onFightWon(rewardMoney * mState.getEarnMult(), rewardFame);
                mCallback.onFightEvent("🏆 KO! Next fight unlocked!");
            });
        } else {
            float bonusMoney = mEnemy.moneyReward * 0.15f;  
            mState.addMoney(bonusMoney);
            mState.saveState();
            SwingUtilities.invokeLater(() -> {
                mCallback.onMoneyChanged(mState.getMoney(), bonusMoney);
                mCallback.onFightEvent("KO! +$" + (int)bonusMoney + " bonus");
            });
        }
        mExec.schedule(this::resetRound, 1800, TimeUnit.MILLISECONDS);
    }

    private void resetRound() {
        if (!mRunning.get()) return;
        mState.resetFight(mEnemy.maxHp);
        mEnemyCharging = false;
        mEnemyIsBlocking = false;
        mState.setEnemyBlocking(false);
        SwingUtilities.invokeLater(() -> {
            mCallback.onRoundReset();
            mCallback.onFightEvent("NEW ROUND — FIGHT!");
        });
    }

    private void scheduleAI() {
        long tickMs = Math.max(120, mEnemy.attackIntervalMs / 12);
        mAITask = mExec.scheduleAtFixedRate(this::aiThink, tickMs, tickMs, TimeUnit.MILLISECONDS);
    }

    private void aiThink() {
        if (!mRunning.get()) return;

        float skill = Math.min(0.95f, 0.20f + mEnemy.fightType * 0.18f);
        float aggression = Math.min(0.90f, 0.30f + mEnemy.fightType * 0.14f);

        if (mRng.nextFloat() < 0.04f) mAIPattern = mRng.nextInt(4);

        long sincePlayerPunch = System.currentTimeMillis() - mLastPlayerPunchMs.get();

        if (sincePlayerPunch < 500 && mRng.nextFloat() < skill) {
            if (mRng.nextFloat() < 0.55f) {
                setEnemyBlock(true, 400 + (int)(mRng.nextFloat() * 300));
            } else {
                int dir = mRng.nextBoolean() ? 1 : -1;
                mState.shiftEnemyPosition(dir);
                notifyPos();
            }
            return;
        }

        if (mEnemyIsBlocking && mRng.nextFloat() < 0.3f) {
            setEnemyBlock(false, 0);
        }

        switch (mAIPattern) {
            case 0: 
                if (mRng.nextFloat() < aggression) executeAttack();
                else if (atSamePos() && mRng.nextFloat() < 0.35f) chasePlayer();
                break;

            case 1:
                if (sincePlayerPunch < 800 && mRng.nextFloat() < skill * 0.8f) {
                    executeAttack(); 
                } else if (mRng.nextFloat() < 0.25f) {
                    setEnemyBlock(true, 500 + mRng.nextInt(400));
                }
                break;

            case 2: 
                int dist = Math.abs(mState.getPosition() - mState.getEnemyPos());
                if (dist > 1 && mRng.nextFloat() < 0.6f) chasePlayer();
                else if (dist <= 1 && mRng.nextFloat() < aggression) executeAttack();
                break;

            case 3: 
                if (!mEnemyIsBlocking && mRng.nextFloat() < 0.3f) {
                    setEnemyBlock(true, 600 + mRng.nextInt(500));
                } else if (mRng.nextFloat() < 0.4f) {
                    triggerEnemyCharge();
                }
                break;
        }
    }

    private void executeAttack() {
        if (mEnemyCharging) return;
        if (mRng.nextFloat() < 0.20f + mEnemy.fightType * 0.06f) {
            triggerEnemyCharge();
        } else {
            attack(false);
        }
    }

    private void triggerEnemyCharge() {
        if (mEnemyCharging) return;
        mEnemyCharging = true;
        SwingUtilities.invokeLater(() -> mCallback.onEnemyTelegraph(MOVE_CHARGED));
        int delay = 550 + mRng.nextInt(250) - mEnemy.fightType * 40;
        delay = Math.max(300, delay);
        final int d = delay;
        mExec.schedule(() -> {
            if (mRunning.get() && mEnemyCharging) {
                attack(true);
                mEnemyCharging = false;
            }
        }, d, TimeUnit.MILLISECONDS);
    }

    private void chasePlayer() {
        int pPos = mState.getPosition();
        int ePos = mState.getEnemyPos();
        int dir = (ePos > pPos) ? -1 : 1; 
        mState.shiftEnemyPosition(dir);
        notifyPos();
    }

    private void setEnemyBlock(boolean blocking, int durationMs) {
        mEnemyIsBlocking = blocking;
        mState.setEnemyBlocking(blocking);
        SwingUtilities.invokeLater(() -> mCallback.onEnemyBlocking(blocking));
        if (blocking && durationMs > 0) {
            mExec.schedule(() -> {
                mEnemyIsBlocking = false;
                mState.setEnemyBlocking(false);
                SwingUtilities.invokeLater(() -> mCallback.onEnemyBlocking(false));
            }, durationMs, TimeUnit.MILLISECONDS);
        }
    }

    private void attack(boolean charged) {
        float dmg = mEnemy.attackDamage * (charged ? GameConstants.CHARGED_DAMAGE_MULT : 1f);
        boolean dead = mState.damagePlayer(dmg);

        if (!mState.isBlocking()) stepTowardCorner(true);

        if (mState.isBlocking()) {
            float earn = GameConstants.BASE_BLOCK_EARN * (charged ? 1.5f : 1f);
            mState.addMoney(earn);
            SwingUtilities.invokeLater(() -> mCallback.onMoneyChanged(mState.getMoney(), earn * mState.getEarnMult()));
        }

        float display = mState.isBlocking()
                ? dmg * GameConstants.BLOCK_DAMAGE_REDUCE * mState.getDefenseMult()
                : dmg * mState.getDefenseMult();
        int type = charged ? MOVE_CHARGED : MOVE_PUNCH;

        SwingUtilities.invokeLater(() -> {
            mCallback.onImpactEffect(true, type, getPlayerScreenX(), getPlayerScreenY());
            mCallback.onScreenShake(charged ? 1.0f : 0.45f);
            mCallback.onPlayerDamaged(mState.getPlayerHp(), display);
            notifyPos();
        });

        if (dead) defeat();
    }

    private void stepTowardCorner(boolean isPlayer) {
        int pos = isPlayer ? mState.getPosition() : mState.getEnemyPos();
        int mid = GameConstants.POSITION_SLOTS / 2;
        int target = (pos <= mid) ? GameConstants.CORNER_SLOT_LEFT : GameConstants.CORNER_SLOT_RIGHT;
        int step = (target < pos) ? -1 : 1;
        if (isPlayer) mState.shiftPosition(step);
        else          mState.shiftEnemyPosition(step);
        SwingUtilities.invokeLater(() -> { mCallback.onKnockback(isPlayer); notifyPos(); });
    }

    private void reactToHit(boolean charged, boolean wasBlocked) {
        if (!wasBlocked && charged && mRng.nextFloat() < 0.5f) {
            mExec.schedule(() -> {
                if (mRunning.get()) { mState.shiftEnemyPosition(mRng.nextBoolean() ? 1 : -1); notifyPos(); }
            }, 200, TimeUnit.MILLISECONDS);
        }
        if (mPunchStreak > 0) mPunchStreak--;
    }

    private void reactToDodge() {
        if (mRng.nextFloat() < 0.5f) {
            mExec.schedule(() -> {
                if (!mRunning.get()) return;
                int diff = mState.getPosition() - mState.getEnemyPos();
                if (Math.abs(diff) > 0) { mState.shiftEnemyPosition(diff > 0 ? -1 : 1); notifyPos(); }
            }, 350, TimeUnit.MILLISECONDS);
        }
    }

    private float handleFame(boolean spam) {
        float delta = spam ? -GameConstants.FAME_LOSE_SPAM : GameConstants.FAME_GAIN_VARIETY;
        mState.addFame(delta);
        return delta;
    }

    private void comboWin() {
        float money = 150f * mState.getEarnMult();
        float fame = GameConstants.FAME_GAIN_COMBO;
        mState.addMoney(money);
        mState.addFame(fame);
        SwingUtilities.invokeLater(() -> {
            mCallback.onComboResult(true);
            mCallback.onMoneyChanged(mState.getMoney(), money);
            mCallback.onFameChanged(mState.getFame(), fame);
        });
    }

    private void comboLose() { SwingUtilities.invokeLater(() -> mCallback.onComboResult(false)); }

    private void defeat() {
        stopFight();
        float loss = mEnemy.moneyPenalty;
        mState.spendMoney(loss);
        mState.saveState();
        SwingUtilities.invokeLater(() -> mCallback.onFightLost(loss));
    }

    public void forceComboComplete() {
        if (mRunning.get()) mExec.submit(this::comboWin);
    }
    public void forceComboFail() {
        if (mRunning.get()) SwingUtilities.invokeLater(() -> mCallback.onComboResult(false));
    }

    private void scheduleCombos() {
        mComboTask = mExec.scheduleAtFixedRate(() -> {
            if (!mRunning.get() || mState.comboActive.get()) return;
            int[] combo = new int[GameConstants.COMBO_LENGTH];
            int[] pool = {MOVE_PUNCH, MOVE_BLOCK, MOVE_DODGE_L, MOVE_DODGE_R};
            for (int i = 0; i < combo.length; i++) combo[i] = pool[mRng.nextInt(pool.length)];
            mState.setActiveComboPrompt(combo);
            SwingUtilities.invokeLater(() -> mCallback.onComboPrompt(combo));
            mExec.schedule(() -> {
                if (mState.comboActive.get()) {
                    mState.cancelCombo();
                    SwingUtilities.invokeLater(() -> mCallback.onComboResult(false));
                }
            }, GameConstants.COMBO_WINDOW_MS, TimeUnit.MILLISECONDS);
        }, GameConstants.COMBO_PROMPT_INTERVAL_MS, GameConstants.COMBO_PROMPT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void scheduleCornerPenalty() {
        mCornerTask = mExec.scheduleAtFixedRate(() -> {
            if (!mRunning.get()) return;
            long entered = mState.cornerEnteredMs.get();
            if (entered > 0 && System.currentTimeMillis() - entered > GameConstants.CORNER_DANGER_MS) {
                mState.addFame(-GameConstants.FAME_LOSE_CORNER);
                SwingUtilities.invokeLater(() -> {
                    mCallback.onFameChanged(mState.getFame(), -GameConstants.FAME_LOSE_CORNER);
                    mCallback.onFightEvent("⚠ CORNER PENALTY!");
                });
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void scheduleFameDrift() {
        mFameTask = mExec.scheduleAtFixedRate(() -> {
            if (!mRunning.get()) return;
            int pos = mState.getPosition();
            if (pos > 0 && pos < GameConstants.POSITION_SLOTS - 1) mState.addFame(0.1f);
        }, 5000, 5000, TimeUnit.MILLISECONDS);
    }

    private void notifyPos() {
        SwingUtilities.invokeLater(() -> mCallback.onPositionChanged(mState.getPosition(), mState.getEnemyPos()));
    }

    private boolean atSamePos() { return mState.getPosition() == mState.getEnemyPos(); }

    private float getPlayerScreenX() { return (mState.getPosition() / (float)GameConstants.POSITION_SLOTS) * 1080f + 270f; }
    private float getPlayerScreenY() { return 1400f; }
    private float getEnemyScreenX()  { return ((GameConstants.POSITION_SLOTS - 1 - mState.getEnemyPos()) / (float)GameConstants.POSITION_SLOTS) * 1080f + 270f; }
    private float getEnemyScreenY()  { return 600f; }

    private void cancelTask(ScheduledFuture<?> t) { if (t != null && !t.isCancelled()) t.cancel(false); }
}
