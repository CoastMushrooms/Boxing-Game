package com.boxergame;

/**
 * All opponents. Drawable fields are resource names resolved by Assets.
 */
public final class EnemyRoster {

    public static final EnemyData SPARRING_PARTNER = new EnemyData(
        "Sparring Partner", GameConstants.FIGHT_TRAINING,
        60f, 8f, 2200, 10f, 400f, 100f, 0,
        "enemy_sparring", "enemy_sparring_punch",
        "enemy_sparring_block", "enemy_sparring_hurt",
        "bg_gym"
    );

    public static final EnemyData DISTRICT_CHAMP = new EnemyData(
        "District Champ", GameConstants.FIGHT_CHAMPIONSHIP,
        90f, 11f, 1900, 20f, 800f, 200f, 0,
        "enemy_district", "enemy_district_punch",
        "enemy_district_block", "enemy_district_hurt",
        "bg_arena_small"
    );

    public static final EnemyData REGIONAL_CHAMP = new EnemyData(
        "Regional Champ", GameConstants.FIGHT_CHAMPIONSHIP,
        130f, 15f, 1700, 35f, 1500f, 400f, 0,
        "enemy_regional", "enemy_regional_punch",
        "enemy_regional_block", "enemy_regional_hurt",
        "bg_arena_mid"
    );

    public static final EnemyData THE_CHAMPION = new EnemyData(
        "The Champion", GameConstants.FIGHT_CHAMPION,
        250f, 22f, 1100, 150f, 10000f, 2000f, 0,
        "enemy_champion", "enemy_champion_punch",
        "enemy_champion_block", "enemy_champion_hurt",
        "bg_arena_championship"
    );

    public static final EnemyData TRAINER_BOB = new EnemyData(
        "Trainer Bob", GameConstants.FIGHT_TRAINING,
        50f, 6f, 2500, 5f, 200f, 50f, 0,
        "enemy_trainer", "enemy_trainer_punch",
        "enemy_trainer_block", "enemy_trainer_hurt",
        "bg_gym"
    );

    public static final EnemyData LOGAN_CLONE = new EnemyData(
        "FightBro99", GameConstants.FIGHT_YOUTUBER,
        100f, 12f, 1800, 30f, 1200f, 300f, 1500,
        "enemy_youtuber", "enemy_youtuber_punch",
        "enemy_youtuber_block", "enemy_youtuber_hurt",
        "bg_arena_mid"
    );

    public static final EnemyData SHADOW = new EnemyData(
        "Shadow", GameConstants.FIGHT_UNDERGROUND,
        140f, 18f, 1400, 60f, 2500f, 700f, 3500,
        "enemy_underground", "enemy_underground_punch",
        "enemy_underground_block", "enemy_underground_hurt",
        "bg_underground"
    );

    public static final EnemyData[] CHAMPIONSHIP_PATH = {
        SPARRING_PARTNER, DISTRICT_CHAMP, REGIONAL_CHAMP, THE_CHAMPION
    };

    public static final EnemyData[] OPTIONAL_FIGHTS = {
        TRAINER_BOB, LOGAN_CLONE, SHADOW
    };

    private EnemyRoster() {}
}
