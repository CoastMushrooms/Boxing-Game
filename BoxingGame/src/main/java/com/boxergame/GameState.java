package com.boxergame;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GameState {

    private volatile float mEnemyMaxHp = 100f;

    private static volatile GameState sInstance;
    public static GameState get() {
        if (sInstance == null) {
            synchronized (GameState.class) {
                if (sInstance == null) sInstance = new GameState();
            }
        }
        return sInstance;
    }

    private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();
    private volatile float mMoney = GameConstants.STARTING_MONEY;
    private volatile float mFame = 0f;
    private volatile float mDamageMult = 1f;
    private volatile float mEarnMult = 1f;
    private volatile float mDefenseMult = 1f;  
    public final AtomicBoolean muscleMemoryUnlocked = new AtomicBoolean(false);
    private volatile int mGlovesLevel = 0;
    private volatile int mShoesLevel = 0;
    private volatile int mArmorLevel = 0;
    private volatile float mPlayerHp = GameConstants.PLAYER_MAX_HEALTH;
    private volatile float mEnemyHp = 100f;
    private volatile int mPosition = 2; 
    private volatile int mEnemyPosition = 2;
    private volatile boolean mIsBlocking = false;
    private final Deque<Integer> mRecentMoves = new ArrayDeque<>();  
    public final AtomicLong cornerEnteredMs = new AtomicLong(-1);
    private final AtomicBoolean[] mUnlocked = {
        new AtomicBoolean(true), 
        new AtomicBoolean(false), 
        new AtomicBoolean(false),  
        new AtomicBoolean(false), 
        new AtomicBoolean(false),  
    };

    public final AtomicInteger currentFightType = new AtomicInteger(-1);
    private volatile int[] mActiveComboPrompt = null;  
    private volatile int mComboProgress = 0;
    public  final AtomicBoolean comboActive = new AtomicBoolean(false);
    public float getMoney() { return mMoney; }

    public void addMoney(float amount) {
        mLock.writeLock().lock();
        try { mMoney = Math.max(0, mMoney + amount * mEarnMult); }
        finally { mLock.writeLock().unlock(); }
    }

    public void addMoneyIdle(float amount) {
        mLock.writeLock().lock();
        try { mMoney = Math.max(0, mMoney + amount * GameConstants.IDLE_EARN_MULT); }
        finally { mLock.writeLock().unlock(); }
    }

    public boolean spendMoney(float cost) {
        mLock.writeLock().lock();
        try {
            if (mMoney >= cost) { mMoney -= cost; return true; }
            return false;
        } finally { mLock.writeLock().unlock(); }
    }

    public float getFame() { return mFame; }

    public void addFame(float delta) {
        mLock.writeLock().lock();
        try { mFame = Math.max(0, Math.min(GameConstants.FAME_MAX, mFame + delta)); }
        finally { mLock.writeLock().unlock(); }
    }

    public float getDamageMult()  { return mDamageMult; }
    public float getEarnMult()    { return mEarnMult; }
    public float getDefenseMult() { return mDefenseMult; }
    public int getGlovesLevel()  { return mGlovesLevel; }
    public int getShoesLevel()   { return mShoesLevel; }
    public int getArmorLevel()   { return mArmorLevel; }

    public void upgradeGloves() {
        mLock.writeLock().lock();
        try { mGlovesLevel++; mDamageMult += 0.25f; mEarnMult += 0.15f; }
        finally { mLock.writeLock().unlock(); }
    }
    public void upgradeShoes() {
        mLock.writeLock().lock();
        try { mShoesLevel++; mEarnMult += 0.20f; }
        finally { mLock.writeLock().unlock(); }
    }
    public void upgradeArmor() {
        mLock.writeLock().lock();
        try { mArmorLevel++; mDefenseMult = Math.max(0.05f, mDefenseMult - 0.15f); }
        finally { mLock.writeLock().unlock(); }
    }
    public float getPlayerHp()   { return mPlayerHp; }
    public float getEnemyHp()    { return mEnemyHp; }
    public int getPosition()   { return mPosition; }
    public int getEnemyPos()   { return mEnemyPosition; }
    public boolean isBlocking()  { return mIsBlocking; }

    public void setBlocking(boolean b) { mIsBlocking = b; }

    public float getEnemyMaxHp() { return mEnemyMaxHp; }

    public void setEnemyMaxHp(float hp) {
        mLock.writeLock().lock();
        try { mEnemyMaxHp = hp; mEnemyHp = hp; }
        finally { mLock.writeLock().unlock(); }
    }

    public void resetFight(float enemyMaxHp) {
        mLock.writeLock().lock();
        try {
            mPlayerHp = GameConstants.PLAYER_MAX_HEALTH;
            mEnemyMaxHp = enemyMaxHp;
            mEnemyHp = enemyMaxHp;
            mPosition = 2;
            mEnemyPosition = 2;
            mIsBlocking = false;
            mRecentMoves.clear();
            cornerEnteredMs.set(-1);
            mActiveComboPrompt = null;
            mComboProgress = 0;
            comboActive.set(false);
        } finally { mLock.writeLock().unlock(); }
    }

    public boolean damagePlayer(float rawDamage) {
        mLock.writeLock().lock();
        try {
            float dmg = mIsBlocking
                    ? rawDamage * GameConstants.BLOCK_DAMAGE_REDUCE * mDefenseMult
                    : rawDamage * mDefenseMult;
            mPlayerHp = Math.max(0, mPlayerHp - dmg);
            if (!mIsBlocking) {
                mPosition = Math.min(GameConstants.POSITION_SLOTS - 1, mPosition + 1);
                checkCorner();
            }
            return mPlayerHp <= 0;
        } finally { mLock.writeLock().unlock(); }
    }

    public boolean damageEnemy(float rawDamage) {
        mLock.writeLock().lock();
        try {
            mEnemyHp = Math.max(0, mEnemyHp - rawDamage * mDamageMult);
            mPosition = Math.max(0, mPosition - 1);
            checkCorner();
            return mEnemyHp <= 0;
        } finally { mLock.writeLock().unlock(); }
    }

    public void shiftPosition(int delta) {
        mLock.writeLock().lock();
        try {
            mPosition = Math.max(0, Math.min(GameConstants.POSITION_SLOTS - 1, mPosition + delta));
            checkCorner();
        } finally { mLock.writeLock().unlock(); }
    }

    private void checkCorner() {
        if (mPosition == GameConstants.CORNER_SLOT_LEFT || mPosition == GameConstants.CORNER_SLOT_RIGHT) {
            if (cornerEnteredMs.get() < 0) cornerEnteredMs.set(System.currentTimeMillis());
        } else {
            cornerEnteredMs.set(-1);
        }
    }

    public boolean recordMove(int moveType) {
        mLock.writeLock().lock();
        try {
            mRecentMoves.addLast(moveType);
            if (mRecentMoves.size() > GameConstants.SPAM_WINDOW)
                mRecentMoves.removeFirst();
            int sameCount = 0;
            for (int m : mRecentMoves) if (m == moveType) sameCount++;
            return sameCount >= GameConstants.SPAM_THRESHOLD;
        } finally { mLock.writeLock().unlock(); }
    }

    public int[] getActiveComboPrompt() { return mActiveComboPrompt; }
    public int getComboProgress()     { return mComboProgress; }

    public void setActiveComboPrompt(int[] moves) {
        mLock.writeLock().lock();
        try { mActiveComboPrompt = moves; mComboProgress = 0; comboActive.set(true); }
        finally { mLock.writeLock().unlock(); }
    }

    public int inputComboMove(int moveType) {
        mLock.writeLock().lock();
        try {
            if (mActiveComboPrompt == null) return 0;
            if (mActiveComboPrompt[mComboProgress] == moveType) {
                mComboProgress++;
                if (mComboProgress >= mActiveComboPrompt.length) {
                    mActiveComboPrompt = null;
                    mComboProgress = 0;
                    comboActive.set(false);
                    return 2; 
                }
                return 1; 
            } else {
                mActiveComboPrompt = null;
                mComboProgress = 0;
                comboActive.set(false);
                return 0;
            }
        } finally { mLock.writeLock().unlock(); }
    }

    public void cancelCombo() {
        mLock.writeLock().lock();
        try { mActiveComboPrompt = null; mComboProgress = 0; comboActive.set(false); }
        finally { mLock.writeLock().unlock(); }
    }
    
    public boolean isUnlocked(int fightType) {
        if (fightType < 0 || fightType >= mUnlocked.length) return false;
        return mUnlocked[fightType].get();
    }

    public void unlock(int fightType) {
        if (fightType >= 0 && fightType < mUnlocked.length)
            mUnlocked[fightType].set(true);
    }

    public void loadState(float money, float fame, boolean muscle,
                          int gloves, int shoes, int armor,
                          boolean[] unlocked) {
        mLock.writeLock().lock();
        try {
            mMoney = money; mFame = fame;
            muscleMemoryUnlocked.set(muscle);
            mGlovesLevel = gloves; mShoesLevel = shoes; mArmorLevel = armor;
            mDamageMult = 1f + gloves * 0.25f;
            mEarnMult = 1f + gloves * 0.15f + shoes * 0.20f;
            mDefenseMult = Math.max(0.05f, 1f - armor * 0.15f);
            for (int i = 0; i < Math.min(unlocked.length, mUnlocked.length); i++)
                mUnlocked[i].set(unlocked[i]);
        } finally { mLock.writeLock().unlock(); }
    }

    public void saveState() {
        mLock.readLock().lock();
        try {
            boolean[] unlocks = new boolean[mUnlocked.length];
            for (int i = 0; i < unlocks.length; i++) {
                unlocks[i] = mUnlocked[i].get();
            }

            SaveManager.saveGameState(
                    mMoney,
                    mFame,
                    muscleMemoryUnlocked.get(),
                    mGlovesLevel,
                    mShoesLevel,
                    mArmorLevel,
                    unlocks
            );
        } finally {
            mLock.readLock().unlock();
        }
    }

    private volatile boolean mEnemyBlocking = false;

    public void setEnemyBlocking(boolean blocking) {
        mEnemyBlocking = blocking;
    }

    public boolean isEnemyBlocking() {
        return mEnemyBlocking;
    }

    public void shiftEnemyPosition(int delta) {
        mLock.writeLock().lock();
        try {
            mEnemyPosition = Math.max(0, Math.min(GameConstants.POSITION_SLOTS - 1, mEnemyPosition + delta));
        } finally {
            mLock.writeLock().unlock();
        }
    }
}
