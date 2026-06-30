package com.boxergame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class FightScreen extends JPanel implements FightEngine.FightCallback {

    public interface FightScreenListener {
        void onFightFinished(boolean won, float moneyDelta, float fameDelta, int fightIndex);
    }

    private final FightScreenListener mListener;
    private final EnemyData           mEnemy;
    private final int mFightIndex;
    private final GameState           mState = GameState.get();

    private FightView    mFightView;
    private JLabel       mTvMoney, mTvFame, mTvEvent, mTvOpponent;
    private JLabel       mTvMusclePassive, mTvAutoFight;
    private JPanel       mComboContainer;
    private JLabel[]     mComboLabels;

    private FightEngine  mEngine;
    private IdleEngine   mIdle;
    private SoundManager mSound;

    private int mCurrentPlayerAnim = FightView.ANIM_NORMAL;
    private int mCurrentEnemyAnim = FightView.ANIM_NORMAL;
    private boolean mWinFired = false;

    private int[] mActiveCombo = null;
    private int mComboStep = 0;

    private static final long AUTO_IDLE_MS = 4000L;
    private Timer mAutoIdleTimer;

    public FightScreen(EnemyData enemy, int fightIndex, FightScreenListener listener) {
        mEnemy = enemy;
        mFightIndex = fightIndex;
        mListener = listener;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        buildUI();
        startFight();
    }

    private void buildUI() {
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        hud.setBackground(new Color(0, 0, 0, 200));

        JButton btnExit = makeButton("", new Color(0x333333), Color.WHITE);
        btnExit.addActionListener(e -> exitFight());
        hud.add(btnExit);

        mTvMoney = hudLabel("$0",    new Color(0xFFD700));
        mTvOpponent = hudLabel(mEnemy.name, Color.WHITE);
        mTvFame = hudLabel("0",  new Color(0xF39C12));
        hud.add(mTvMoney);
        hud.add(Box.createHorizontalStrut(20));
        hud.add(mTvOpponent);
        hud.add(Box.createHorizontalStrut(20));
        hud.add(mTvFame);

        mFightView = new FightView();
        mFightView.setArenaBackground(mEnemy.backgroundDrawable);
        mFightView.setPlayerSprites("player_normal","player_punch","player_block","player_charged","player_hurt");
        mFightView.setEnemySprites(mEnemy.drawableNormal, mEnemy.drawablePunch,
                                   mEnemy.drawableBlock,  mEnemy.drawableHurt);

        mTvEvent = new JLabel("", SwingConstants.CENTER);
        mTvEvent.setFont(new Font("SansSerif", Font.BOLD, 26));
        mTvEvent.setForeground(Color.WHITE);
        mTvEvent.setOpaque(true);
        mTvEvent.setBackground(new Color(0, 0, 0, 180));
        mTvEvent.setVisible(false);

        mTvMusclePassive = new JLabel("", SwingConstants.RIGHT);
        mTvMusclePassive.setFont(new Font("SansSerif", Font.ITALIC, 11));
        mTvMusclePassive.setForeground(new Color(200, 200, 200, 150));
        mTvMusclePassive.setVisible(false);

        mTvAutoFight = new JLabel("Muscle Memory fighting…", SwingConstants.RIGHT);
        mTvAutoFight.setFont(new Font("SansSerif", Font.PLAIN, 11));
        mTvAutoFight.setForeground(new Color(0, 255, 153, 170));
        mTvAutoFight.setVisible(false);

        JLayeredPane layers = new JLayeredPane();
        layers.setPreferredSize(new Dimension(720, 960));
        mFightView.setBounds(0, 0, 720, 960);
        mTvEvent.setBounds(160, 420, 400, 60);
        mTvMusclePassive.setBounds(580, 60, 130, 20);
        mTvAutoFight.setBounds(560, 82, 150, 20);
        layers.add(mFightView,        JLayeredPane.DEFAULT_LAYER);
        layers.add(mTvEvent,          JLayeredPane.PALETTE_LAYER);
        layers.add(mTvMusclePassive,  JLayeredPane.PALETTE_LAYER);
        layers.add(mTvAutoFight,      JLayeredPane.PALETTE_LAYER);

        mComboContainer = new JPanel(new FlowLayout());
        mComboContainer.setBackground(new Color(0, 0, 0, 220));
        mComboContainer.setVisible(false);
        JLabel comboTitle = new JLabel("⚡ COMBO! Do these moves:");
        comboTitle.setForeground(new Color(0xFFD700));
        comboTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        mComboContainer.add(comboTitle);
        mComboLabels = new JLabel[3];
        for (int i = 0; i < 3; i++) {
            mComboLabels[i] = new JLabel("?", SwingConstants.CENTER);
            mComboLabels[i].setFont(new Font("SansSerif", Font.BOLD, 14));
            mComboLabels[i].setForeground(Color.WHITE);
            mComboLabels[i].setOpaque(true);
            mComboLabels[i].setBackground(new Color(255,255,255,68));
            mComboLabels[i].setPreferredSize(new Dimension(120, 40));
            mComboContainer.add(mComboLabels[i]);
        }

        JPanel buttons = buildButtonPanel();
        JPanel centre = new JPanel(new BorderLayout());
        centre.add(layers,           BorderLayout.CENTER);
        centre.add(mComboContainer,  BorderLayout.SOUTH);
        add(hud,     BorderLayout.NORTH);
        add(centre,  BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 4));
        panel.setBackground(new Color(17, 17, 34, 220));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));

        JPanel row1 = new JPanel(new GridLayout(1, 2, 4, 0));
        row1.setOpaque(false);
        JButton btnPunch = makeButton("PUNCH",  new Color(0x3498DB, false), Color.WHITE);
        JButton btnCharge = makeButton("CHARGE", new Color(0xFFD700), Color.BLACK);
        btnPunch .addActionListener(e -> doPlayerPunch());
        btnCharge.addActionListener(e -> doPlayerCharge());
        row1.add(btnPunch);
        row1.add(btnCharge);

        JPanel row2 = new JPanel(new GridLayout(1, 3, 4, 0));
        row2.setOpaque(false);
        JButton btnDodgeL = makeButton("DODGE",  new Color(0x2ECC40), Color.WHITE);
        JButton btnBlock = makeButton("BLOCK",  new Color(0xE74C3C), Color.WHITE);
        JButton btnDodgeR = makeButton("DODGE →",   new Color(0x2ECC40), Color.WHITE);
        btnDodgeL.addActionListener(e -> doPlayerDodge(-1));
        btnBlock .addActionListener(e -> doPlayerBlock());
        btnDodgeR.addActionListener(e -> doPlayerDodge(1));
        row2.add(btnDodgeL);
        row2.add(btnBlock);
        row2.add(btnDodgeR);

        panel.add(row1);
        panel.add(row2);

        setupKeyBindings(btnPunch, btnCharge, btnBlock, btnDodgeL, btnDodgeR);

        return panel;
    }

    private void setupKeyBindings(JButton punch, JButton charge, JButton block,
                                   JButton dodgeL, JButton dodgeR) {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke('p'), "punch");
        im.put(KeyStroke.getKeyStroke('c'), "charge");
        im.put(KeyStroke.getKeyStroke('b'), "block");
        im.put(KeyStroke.getKeyStroke('l'), "dodgeL");
        im.put(KeyStroke.getKeyStroke('r'), "dodgeR");
        am.put("punch",  new AbstractAction() { public void actionPerformed(ActionEvent e) { doPlayerPunch(); }});
        am.put("charge", new AbstractAction() { public void actionPerformed(ActionEvent e) { doPlayerCharge(); }});
        am.put("block",  new AbstractAction() { public void actionPerformed(ActionEvent e) { doPlayerBlock(); }});
        am.put("dodgeL", new AbstractAction() { public void actionPerformed(ActionEvent e) { doPlayerDodge(-1); }});
        am.put("dodgeR", new AbstractAction() { public void actionPerformed(ActionEvent e) { doPlayerDodge(1); }});
    }

    private void startFight() {
        mSound = new SoundManager();
        mIdle = new IdleEngine();
        mEngine = new FightEngine();
        String music = (mEnemy.fightType == GameConstants.FIGHT_CHAMPION)
                       ? "music_champion" : "music_fight";
        mSound.playMusic(music, true);
        mEngine.startFight(mEnemy, this);
        refreshHud();
        scheduleAutoFightTimer();
    }

    private void exitFight() {
        if (mAutoIdleTimer != null) mAutoIdleTimer.stop();
        if (mIdle   != null) mIdle.stopAutoFight();
        if (mEngine != null) mEngine.destroy();
        if (mSound  != null) { mSound.stopMusic(); mSound.destroy(); }
        mListener.onFightFinished(mWinFired, 0, 0, mFightIndex);
    }

    private void onPlayerAction() {
        if (mAutoIdleTimer != null) mAutoIdleTimer.restart();
        if (mIdle != null) mIdle.stopAutoFight();
        if (mTvAutoFight != null) mTvAutoFight.setVisible(false);
    }

    private void doPlayerPunch() {
        onPlayerAction();
        mEngine.playerPunch();
        triggerPlayerAnim(FightView.ANIM_PUNCH);
        mSound.playSfx("sfx_punch");
        advanceComboIfActive(FightEngine.MOVE_PUNCH);
    }

    private void doPlayerCharge() {
        onPlayerAction();
        mEngine.playerChargedPunch();
        triggerPlayerAnim(FightView.ANIM_CHARGED);
        mSound.playSfx("sfx_punch_charged");
        advanceComboIfActive(FightEngine.MOVE_CHARGED);
    }

    private void doPlayerBlock() {
        onPlayerAction();
        mState.setBlocking(true);
        mEngine.playerBlock(true);
        triggerPlayerAnim(FightView.ANIM_BLOCK);
        mSound.playSfx("sfx_block");
        advanceComboIfActive(FightEngine.MOVE_BLOCK);
        Timer t = new Timer(600, e -> { mState.setBlocking(false); mEngine.playerBlock(false); });
        t.setRepeats(false); t.start();
    }

    private void doPlayerDodge(int dir) {
        onPlayerAction();
        mEngine.playerDodge(dir);
        mSound.playSfx("sfx_dodge");
        advanceComboIfActive(dir < 0 ? FightEngine.MOVE_DODGE_L : FightEngine.MOVE_DODGE_R);
    }

    private void advanceComboIfActive(int move) {
        if (mActiveCombo == null || mComboStep >= mActiveCombo.length) return;
        if (mActiveCombo[mComboStep] == move) {
            final int step = mComboStep;
            if (step < mComboLabels.length) {
                mComboLabels[step].setForeground(new Color(0x2ECC40));
            }
            mComboStep++;
            if (mComboStep >= mActiveCombo.length) {
                mActiveCombo = null; mComboStep = 0;
                mComboContainer.setVisible(false);
                mEngine.forceComboComplete();
            }
        } else {
            mActiveCombo = null; mComboStep = 0;
            mComboContainer.setVisible(false);
            mEngine.forceComboFail();
        }
    }

    private void scheduleAutoFightTimer() {
        if (mAutoIdleTimer != null) mAutoIdleTimer.stop();
        if (!mState.muscleMemoryUnlocked.get()) return;
        mAutoIdleTimer = new Timer((int) AUTO_IDLE_MS, e -> startAutoFight());
        mAutoIdleTimer.setRepeats(false);
        mAutoIdleTimer.start();
    }

    private void startAutoFight() {
        if (!mState.muscleMemoryUnlocked.get()) return;
        mTvAutoFight.setVisible(true);
        mIdle.startAutoFight(action -> {
            switch (action) {
                case FightEngine.MOVE_PUNCH:   mEngine.playerPunch(); triggerPlayerAnim(FightView.ANIM_PUNCH);    mSound.playSfx("sfx_punch"); break;
                case FightEngine.MOVE_CHARGED: mEngine.playerChargedPunch(); triggerPlayerAnim(FightView.ANIM_CHARGED); mSound.playSfx("sfx_punch_charged"); break;
                case FightEngine.MOVE_BLOCK:
                    mState.setBlocking(true); mEngine.playerBlock(true); triggerPlayerAnim(FightView.ANIM_BLOCK);
                    Timer bt = new Timer(500, ev -> { mState.setBlocking(false); mEngine.playerBlock(false); });
                    bt.setRepeats(false); bt.start(); break;
                case FightEngine.MOVE_DODGE_L: mEngine.playerDodge(-1); break;
                case FightEngine.MOVE_DODGE_R: mEngine.playerDodge(1);  break;
            }
        });
    }

    @Override public void onPlayerDamaged(float hp, float dmg) {
        triggerPlayerAnim(FightView.ANIM_HURT);
        if (mFightView != null) mFightView.showFloatText("-" + (int) dmg, Color.RED);
        refreshFightView();
    }

    @Override public void onEnemyDamaged(float hp, float dmg) {
        triggerEnemyAnim(FightView.ANIM_HURT);
        if (mFightView != null) mFightView.showFloatText("-" + (int) dmg, Color.YELLOW);
        refreshFightView();
    }

    @Override public void onEnemyBlocking(boolean blocking) {
        if (blocking) { triggerEnemyAnim(FightView.ANIM_BLOCK); showEvent("🛡 Enemy blocking!"); }
        else           { mCurrentEnemyAnim = FightView.ANIM_NORMAL; refreshFightView(); }
    }

    @Override public void onPositionChanged(int pp, int ep) { refreshFightView(); }

    @Override public void onFameChanged(float fame, float delta) {
        mTvFame.setText("" + (int) fame);
        if (delta > 0 && mFightView != null) mFightView.showFloatText("+" + (int) delta + " Fame", new Color(0xFFD700));
    }

    @Override public void onMoneyChanged(float money, float delta) {
        mTvMoney.setText("$" + (int) money);
        if (delta > 0 && mFightView != null) mFightView.showFloatText("+$" + (int) delta, new Color(0x2ECC40));
        if (mState.muscleMemoryUnlocked.get()) {
            mTvMusclePassive.setText("+$" + (int)(IdleEngine.FIGHT_PASSIVE_BONUS * mState.getEarnMult()));
            mTvMusclePassive.setVisible(true);
            Timer t = new Timer(800, e -> mTvMusclePassive.setVisible(false));
            t.setRepeats(false); t.start();
        }
    }

    @Override public void onComboPrompt(int[] moves) {
        mActiveCombo = moves; mComboStep = 0;
        mComboContainer.setVisible(true);
        for (int i = 0; i < mComboLabels.length && i < moves.length; i++) {
            mComboLabels[i].setText(moveName(moves[i]));
            mComboLabels[i].setForeground(new Color(255, 255, 255, 153));
        }
        mSound.playSfx("sfx_combo_hit");
    }

    @Override public void onComboResult(boolean success) {
        if (success) { mSound.playSfx("sfx_combo_complete"); showEvent("🏅 COMBO COMPLETE!"); }
        else           { showEvent("Combo missed"); }
        mActiveCombo = null; mComboStep = 0;
        mComboContainer.setVisible(false);
    }

    @Override public void onFightWon(float money, float fame) {
        if (!mWinFired) {
            mWinFired = true;
            mSound.playSfx("sfx_win");
            mSound.playSfx("sfx_crowd_cheer");
            showEvent("KO! Next fight unlocked!");
            refreshHud();
            Timer t = new Timer(2000, e -> {
                if (mSound != null) { mSound.stopMusic(); mSound.destroy(); }
                mListener.onFightFinished(true, mEnemy.moneyReward, mEnemy.fameReward, mFightIndex);
            });
            t.setRepeats(false); t.start();
        }
    }

    @Override public void onRoundReset() { refreshFightView(); refreshHud(); }

    @Override public void onKnockback(boolean playerKnockedBack) {
        if (playerKnockedBack) triggerPlayerAnim(FightView.ANIM_HURT);
        else                   triggerEnemyAnim(FightView.ANIM_HURT);
        refreshFightView();
    }

    @Override public void onFightLost(float lost) {
        mSound.playSfx("sfx_lose");
        mSound.stopMusic();
        Timer t = new Timer(1200, e -> {
            mSound.destroy();
            mListener.onFightFinished(false, lost, 0, mFightIndex);
        });
        t.setRepeats(false); t.start();
    }

    @Override public void onFightEvent(String text) { showEvent(text); }

    @Override public void onEnemyTelegraph(int type) {
        triggerEnemyAnim(FightView.ANIM_CHARGED);
        showEvent("INCOMING POWER SHOT!");
    }

    @Override public void onImpactEffect(boolean hitPlayer, int moveType, float x, float y) {
        if (mFightView != null) mFightView.triggerImpact(x, y, moveType == FightEngine.MOVE_CHARGED);
    }

    @Override public void onScreenShake(float intensity) {
        if (mFightView != null) mFightView.triggerScreenShake(intensity);
    }

    private void refreshHud() {
        mTvMoney.setText("$" + (int) mState.getMoney());
        mTvFame .setText("" + (int) mState.getFame());
    }

    private void refreshFightView() {
        if (mFightView == null) return;
        mFightView.updateFightState(
                mState.getPlayerHp() / GameConstants.PLAYER_MAX_HEALTH,
                mState.getEnemyHp()  / mState.getEnemyMaxHp(),
                mState.getPosition(), mState.getEnemyPos(),
                mCurrentPlayerAnim, mCurrentEnemyAnim);
    }

    private void showEvent(String text) {
        mTvEvent.setText(text);
        mTvEvent.setVisible(true);
        Timer t = new Timer(1800, e -> mTvEvent.setVisible(false));
        t.setRepeats(false); t.start();
    }

    private void triggerPlayerAnim(int anim) {
        mCurrentPlayerAnim = anim; refreshFightView();
        Timer t = new Timer(300, e -> { mCurrentPlayerAnim = FightView.ANIM_NORMAL; refreshFightView(); });
        t.setRepeats(false); t.start();
    }

    private void triggerEnemyAnim(int anim) {
        mCurrentEnemyAnim = anim; refreshFightView();
        Timer t = new Timer(300, e -> { mCurrentEnemyAnim = FightView.ANIM_NORMAL; refreshFightView(); });
        t.setRepeats(false); t.start();
    }

    private String moveName(int move) {
        switch (move) {
            case FightEngine.MOVE_PUNCH:   return "PUNCH";
            case FightEngine.MOVE_BLOCK:   return "BLOCK";
            case FightEngine.MOVE_DODGE_L: return "DODGE";
            case FightEngine.MOVE_DODGE_R: return "DODGE";
            case FightEngine.MOVE_CHARGED: return "CHARGE";
            default: return "?";
        }
    }

    private JLabel hudLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 15));
        l.setForeground(color);
        return l;
    }

    private JButton makeButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(0, 52));
        return b;
    }
}
