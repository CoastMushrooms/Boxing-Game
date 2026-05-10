package com.boxergame;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * Desktop SaveManager — replaces Android SharedPreferences with java.util.prefs.Preferences.
 * Persists to OS registry / user home directory on all platforms.
 */
public class SaveManager {

    private static final String NODE          = "com/boxergame";
    private static final String K_MONEY       = "money";
    private static final String K_FAME        = "fame";
    private static final String K_MUSCLE      = "muscle";
    private static final String K_GLOVES      = "gloves";
    private static final String K_SHOES       = "shoes";
    private static final String K_ARMOR       = "armor";
    private static final String K_UNL_PRE     = "unlocked_";
    private static final String K_CHAMP_IDX   = "champ_idx";
    private static final String K_LAST_ONLINE = "last_online_ms";
    private static final String K_SHOWN_INSTR = "shown_instructions";

    private static final Preferences sPrefs   = Preferences.userRoot().node(NODE);
    private final        Preferences mPrefs   = sPrefs;

    public SaveManager() { /* no Context needed */ }

    // ── Static helpers (called from GameState / FightEngine) ─────────────────

    /** Called from GameState.saveState() — saves economy + upgrades. */
    public static void saveGameState(float money, float fame, boolean muscle,
                                     int gloves, int shoes, int armor,
                                     boolean[] unlocked) {
        sPrefs.putFloat  (K_MONEY,  money);
        sPrefs.putFloat  (K_FAME,   fame);
        sPrefs.putBoolean(K_MUSCLE, muscle);
        sPrefs.putInt    (K_GLOVES, gloves);
        sPrefs.putInt    (K_SHOES,  shoes);
        sPrefs.putInt    (K_ARMOR,  armor);
        for (int i = 0; i < unlocked.length; i++)
            sPrefs.putBoolean(K_UNL_PRE + i, unlocked[i]);
        sPrefs.putLong(K_LAST_ONLINE, System.currentTimeMillis());
        flush();
    }

    /** Called from FightEngine immediately on enemy death — saves championship progress. */
    public static void saveChampionshipProgress(GameState state, EnemyData defeated) {
        int defeatedIndex = -1;
        for (int i = 0; i < EnemyRoster.CHAMPIONSHIP_PATH.length; i++) {
            if (EnemyRoster.CHAMPIONSHIP_PATH[i] == defeated) { defeatedIndex = i; break; }
        }
        if (defeatedIndex < 0) return;  // optional fight — no progress to advance

        int current = sPrefs.getInt(K_CHAMP_IDX, 0);
        int next    = Math.min(EnemyRoster.CHAMPIONSHIP_PATH.length - 1, defeatedIndex + 1);
        if (next > current) {
            sPrefs.putInt(K_CHAMP_IDX, next);
            sPrefs.putFloat(K_MONEY, state.getMoney());
            sPrefs.putFloat(K_FAME,  state.getFame());
            sPrefs.putLong(K_LAST_ONLINE, System.currentTimeMillis());
            flush();
        }
    }

    // ── Instance methods (called from lobby / shop) ───────────────────────────

    /** Full save including championship progress. */
    public void save(int championshipProgress) {
        GameState s = GameState.get();
        mPrefs.putFloat  (K_MONEY,   s.getMoney());
        mPrefs.putFloat  (K_FAME,    s.getFame());
        mPrefs.putBoolean(K_MUSCLE,  s.muscleMemoryUnlocked.get());
        mPrefs.putInt    (K_GLOVES,  s.getGlovesLevel());
        mPrefs.putInt    (K_SHOES,   s.getShoesLevel());
        mPrefs.putInt    (K_ARMOR,   s.getArmorLevel());
        mPrefs.putInt    (K_CHAMP_IDX, championshipProgress);
        for (int i = 0; i < 5; i++)
            mPrefs.putBoolean(K_UNL_PRE + i, s.isUnlocked(i));
        mPrefs.putLong(K_LAST_ONLINE, System.currentTimeMillis());
        flush();
    }

    /** Load all saved state into GameState. Returns championship progress index. */
    public int load() {
        GameState s = GameState.get();
        float   money  = mPrefs.getFloat  (K_MONEY,  GameConstants.STARTING_MONEY);
        float   fame   = mPrefs.getFloat  (K_FAME,   0f);
        boolean muscle = mPrefs.getBoolean(K_MUSCLE, false);
        int     gloves = mPrefs.getInt    (K_GLOVES, 0);
        int     shoes  = mPrefs.getInt    (K_SHOES,  0);
        int     armor  = mPrefs.getInt    (K_ARMOR,  0);
        boolean[] unlocked = new boolean[5];
        unlocked[0] = true;
        for (int i = 1; i < 5; i++)
            unlocked[i] = mPrefs.getBoolean(K_UNL_PRE + i, false);
        s.loadState(money, fame, muscle, gloves, shoes, armor, unlocked);
        return mPrefs.getInt(K_CHAMP_IDX, 0);
    }

    public long getLastOnlineMs() {
        return mPrefs.getLong(K_LAST_ONLINE, 0L);
    }

    public boolean hasShownInstructions() {
        return mPrefs.getBoolean(K_SHOWN_INSTR, false);
    }

    public void setShownInstructions() {
        mPrefs.putBoolean(K_SHOWN_INSTR, true);
        flush();
    }

    public void reset() {
        try { mPrefs.clear(); flush(); } catch (Exception ignored) {}
    }

    private static void flush() {
        try { sPrefs.flush(); } catch (BackingStoreException ignored) {}
    }
}
