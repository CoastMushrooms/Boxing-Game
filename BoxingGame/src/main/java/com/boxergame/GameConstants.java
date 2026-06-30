package com.boxergame;

public final class GameConstants {

    public static final int STARTING_MONEY = 200;
    public static final float BASE_PUNCH_EARN = 2f;       
    public static final float BASE_BLOCK_EARN = 1f;      
    public static final float CHARGED_PUNCH_EARN = 6f;       
    public static final float FAME_GAIN_VARIETY = 0.4f;  
    public static final float FAME_GAIN_COMBO = 8f;     
    public static final float FAME_LOSE_SPAM = 3f;       
    public static final float FAME_LOSE_CORNER = 4f;       
    public static final float FAME_CHARGED_BONUS = 2f;       
    public static final float FAME_MAX = 1000f;
    public static final float FAME_HIGH_THRESHOLD = 700f;
    public static final float MONEY_HIGH_THRESHOLD = 50000f;
    public static final float BASE_PLAYER_DAMAGE = 10f;     
    public static final float BASE_ENEMY_DAMAGE = 12f;
    public static final float BLOCK_DAMAGE_REDUCE = 0.25f;   
    public static final float CHARGED_DAMAGE_MULT = 2.2f;
    public static final float PLAYER_MAX_HEALTH = 100f;
    public static final int POSITION_SLOTS = 5;
    public static final int CORNER_SLOT_LEFT = 0;
    public static final int CORNER_SLOT_RIGHT = 4;
    public static final long  CORNER_DANGER_MS = 2000;
    public static final long  PUNCH_COOLDOWN_MS = 600;     
    public static final long  CHARGED_COOLDOWN_MS = 1800;
    public static final int SPAM_WINDOW = 4;
    public static final int SPAM_THRESHOLD = 3;       
    public static final long  IDLE_MOVE_INTERVAL_MS = 2000;
    public static final int MUSCLE_MEMORY_COST = 5000;
    public static final float IDLE_EARN_MULT = 0.5f;
    public static final long  CHARGE_HOLD_MS = 900;
    public static final long  COMBO_PROMPT_INTERVAL_MS = 12000; 
    public static final long  COMBO_WINDOW_MS = 5000;
    public static final int COMBO_LENGTH = 3;
    public static final long  ENEMY_ATTACK_BASE_MS = 1600;
    public static final int FIGHT_TRAINING = 0;
    public static final int FIGHT_YOUTUBER = 1;
    public static final int FIGHT_UNDERGROUND = 2;
    public static final int FIGHT_CHAMPIONSHIP = 3;
    public static final int FIGHT_CHAMPION = 4;

    private GameConstants() {}
}
