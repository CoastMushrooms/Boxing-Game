package com.boxergame;

public class EnemyData {

    public final String name;
    public final int fightType;
    public final float  maxHp;
    public final float  attackDamage;
    public final long   attackIntervalMs;
    public final float  fameReward;
    public final float  moneyReward;
    public final float  moneyPenalty;
    public final int unlockCost;
    public final String drawableNormal;
    public final String drawablePunch;
    public final String drawableBlock;
    public final String drawableHurt;
    public final String backgroundDrawable;

    public EnemyData(String name, int fightType, float maxHp, float attackDamage,
                     long attackIntervalMs, float fameReward, float moneyReward,
                     float moneyPenalty, int unlockCost,
                     String drawableNormal, String drawablePunch,
                     String drawableBlock, String drawableHurt,
                     String backgroundDrawable) {
        this.name = name;
        this.fightType = fightType;
        this.maxHp = maxHp;
        this.attackDamage = attackDamage;
        this.attackIntervalMs = attackIntervalMs;
        this.fameReward = fameReward;
        this.moneyReward = moneyReward;
        this.moneyPenalty = moneyPenalty;
        this.unlockCost = unlockCost;
        this.drawableNormal = drawableNormal;
        this.drawablePunch = drawablePunch;
        this.drawableBlock = drawableBlock;
        this.drawableHurt = drawableHurt;
        this.backgroundDrawable = backgroundDrawable;
    }
}
