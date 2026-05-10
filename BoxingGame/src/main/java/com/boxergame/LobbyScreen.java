package com.boxergame;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Desktop replacement for MainActivity.
 * Shows money/fame HUD, the fight list, shop button, and how-to-play.
 */
public class LobbyScreen extends JPanel {

    public interface LobbyListener {
        void onFightSelected(int index);
        void onShopClicked();
    }

    private final LobbyListener mListener;
    private final GameState     mState   = GameState.get();
    private final SaveManager   mSave;
    private final IdleEngine    mIdle;
    private final SoundManager  mSound;

    private int  mChampionshipProgress;

    private JLabel  mTvMoney, mTvFame, mTvIdleStatus;
    private JPanel  mFightList;

    private static final Color BG         = new Color(0x0D0D1A);
    private static final Color GOLD       = new Color(0xFFD700);
    private static final Color FAME_CLR   = new Color(0xF39C12);
    private static final Color GREEN      = new Color(0x2ECC40);
    private static final Color BTN_FIGHT  = new Color(0xE53935);
    private static final Color BTN_LOCKED = new Color(0x444444);
    private static final Color BTN_BUY    = new Color(0x1A7A3A);
    private static final Color PANEL_BG   = new Color(0x16213E);

    public LobbyScreen(SaveManager save, SoundManager sound, LobbyListener listener) {
        mSave     = save;
        mSound    = sound;
        mListener = listener;
        mIdle     = new IdleEngine();

        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    public void onResume() {
        mChampionshipProgress = mSave.load();
        mSound.playMusic("music_lobby", true);
        refreshHud();
        buildFightList();
        if (mState.muscleMemoryUnlocked.get() && !mIdle.isActive()) startIdle();
    }

    public void onPause() {
        mSound.pauseMusic();
        mSave.save(mChampionshipProgress);
        mIdle.stop();
    }

    public void destroy() {
        mIdle.destroy();
    }

    public void setChampionshipProgress(int p) {
        mChampionshipProgress = p;
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        // HUD bar
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        hud.setBackground(new Color(0x16213E));
        hud.setBorder(new EmptyBorder(4, 12, 4, 12));

        mTvMoney = hudLabel("$0", GOLD, 18);
        mTvFame  = hudLabel("⭐ 0", FAME_CLR, 18);
        hud.add(mTvMoney);
        hud.add(new JLabel("  |  "));
        hud.add(mTvFame);

        // Idle status
        mTvIdleStatus = new JLabel("", SwingConstants.CENTER);
        mTvIdleStatus.setFont(new Font("SansSerif", Font.ITALIC, 13));
        mTvIdleStatus.setForeground(GREEN);
        mTvIdleStatus.setOpaque(true);
        mTvIdleStatus.setBackground(BG);

        // Title
        JLabel title = new JLabel("LAST SEASON", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(10, 0, 8, 0));

        // Fight list
        mFightList = new JPanel();
        mFightList.setLayout(new BoxLayout(mFightList, BoxLayout.Y_AXIS));
        mFightList.setBackground(BG);
        mFightList.setBorder(new EmptyBorder(8, 12, 8, 12));
        JScrollPane scroll = new JScrollPane(mFightList);
        scroll.setBackground(BG);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);

        // Bottom buttons
        JPanel bottom = new JPanel(new GridLayout(1, 2, 8, 0));
        bottom.setBackground(BG);
        bottom.setBorder(new EmptyBorder(10, 12, 12, 12));

        JButton btnShop = makeStyledButton("🛒  SHOP", BTN_FIGHT);
        JButton btnHelp = makeStyledButton("❓  HOW TO PLAY", new Color(0xB8860B));
        btnShop.addActionListener(e -> mListener.onShopClicked());
        btnHelp.addActionListener(e -> showInstructions());
        bottom.add(btnShop);
        bottom.add(btnHelp);

        // Top composite
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG);
        top.add(hud,           BorderLayout.NORTH);
        top.add(mTvIdleStatus, BorderLayout.CENTER);
        top.add(title,         BorderLayout.SOUTH);

        add(top,    BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    // ── Fight list ────────────────────────────────────────────────────────────

    void buildFightList() {
        mFightList.removeAll();

        // Championship path
        for (int i = 0; i < EnemyRoster.CHAMPIONSHIP_PATH.length; i++) {
            EnemyData e        = EnemyRoster.CHAMPIONSHIP_PATH[i];
            boolean   unlocked = (i <= mChampionshipProgress);
            mFightList.add(makeFightRow(e, i, unlocked, false));
            mFightList.add(Box.createVerticalStrut(6));
        }

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x333355));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        mFightList.add(sep);
        mFightList.add(Box.createVerticalStrut(6));

        // Optional fights
        for (int i = 0; i < EnemyRoster.OPTIONAL_FIGHTS.length; i++) {
            EnemyData e        = EnemyRoster.OPTIONAL_FIGHTS[i];
            int       idx      = EnemyRoster.CHAMPIONSHIP_PATH.length + i;
            boolean   unlocked = mState.isUnlocked(e.fightType);
            boolean   buyable  = !unlocked && e.unlockCost > 0;
            mFightList.add(makeFightRow(e, idx, unlocked, buyable));
            mFightList.add(Box.createVerticalStrut(6));
        }

        mFightList.revalidate();
        mFightList.repaint();
    }

    private JPanel makeFightRow(EnemyData e, int idx, boolean unlocked, boolean buyable) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setBackground(PANEL_BG);
        row.setBorder(new CompoundBorder(
            new LineBorder(new Color(0x2a2a4a), 1, true),
            new EmptyBorder(8, 12, 8, 12)));

        // Left: name + rewards
        JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
        info.setOpaque(false);

        String nameText = unlocked ? e.name : (buyable ? "🔒 " + e.name : "🔒 " + e.name);
        JLabel nameLabel = new JLabel(nameText);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        nameLabel.setForeground(unlocked ? Color.WHITE : new Color(0x888888));

        JLabel rewardLabel = new JLabel(unlocked
            ? "💰 +" + (int)e.moneyReward + "   ⭐ +" + (int)e.fameReward
            : (buyable ? "Unlock for $" + e.unlockCost : "Beat previous opponent first"));
        rewardLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rewardLabel.setForeground(unlocked ? new Color(0xAAAAAA) : new Color(0x666666));

        info.add(nameLabel);
        info.add(rewardLabel);

        // Right: action button
        JButton btn;
        if (unlocked) {
            btn = makeStyledButton("FIGHT ▶", BTN_FIGHT);
            btn.addActionListener(ev -> mListener.onFightSelected(idx));
        } else if (buyable) {
            btn = makeStyledButton("$" + e.unlockCost, BTN_BUY);
            btn.addActionListener(ev -> tryUnlock(e, idx));
        } else {
            btn = makeStyledButton("LOCKED", BTN_LOCKED);
            btn.setEnabled(false);
        }
        btn.setPreferredSize(new Dimension(110, 42));

        row.add(info, BorderLayout.CENTER);
        row.add(btn,  BorderLayout.EAST);
        return row;
    }

    private void tryUnlock(EnemyData e, int idx) {
        if (mState.spendMoney(e.unlockCost)) {
            mState.unlock(e.fightType);
            mSave.save(mChampionshipProgress);
            JOptionPane.showMessageDialog(this, e.name + " unlocked!", "Unlocked!", JOptionPane.INFORMATION_MESSAGE);
            buildFightList();
            refreshHud();
        } else {
            JOptionPane.showMessageDialog(this, "Need $" + e.unlockCost + " to unlock!", "Not enough money", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ── HUD ───────────────────────────────────────────────────────────────────

    void refreshHud() {
        mTvMoney.setText("$" + (int) mState.getMoney());
        mTvFame .setText("⭐ " + (int) mState.getFame());
        boolean active = mState.muscleMemoryUnlocked.get() && mIdle.isActive();
        mTvIdleStatus.setText(active ? "💪 Muscle Memory: ACTIVE" : " ");
    }

    // ── Idle ──────────────────────────────────────────────────────────────────

    private void startIdle() {
        mIdle.start((newMoney, earned) -> SwingUtilities.invokeLater(() -> {
            mTvMoney.setText("$" + (int) newMoney);
            mTvIdleStatus.setText("💪 Muscle Memory: +$" + (int) earned);
        }));
    }

    // ── Instructions dialog ───────────────────────────────────────────────────

    static void showInstructions(Component parent) {
        String msg =
            "CONTROLS\n" +
            "──────────────────────────────\n" +
            "Use the buttons at the bottom of the fight screen\n" +
            "OR use keyboard shortcuts:\n\n" +
            "  [P]  PUNCH      — Quick jab\n" +
            "  [C]  CHARGE     — Power shot\n" +
            "  [B]  BLOCK      — Reduce incoming damage to ~25%\n" +
            "  [L]  DODGE LEFT — Sidestep left\n" +
            "  [R]  DODGE RIGHT— Sidestep right\n\n" +
            "FIGHT TIPS\n" +
            "──────────────────────────────\n" +
            "• Blocking reduces damage to ~25%.\n" +
            "• Skilled enemies will block your punches too!\n" +
            "• Staying in a corner costs you Fame.\n" +
            "• Enemy telegraphs a charged punch — block or dodge!\n" +
            "• Spamming the same move loses Fame.\n\n" +
            "COMBOS\n" +
            "──────────────────────────────\n" +
            "A COMBO prompt appears every ~12 seconds.\n" +
            "Do the 3 shown moves in order within 5 seconds for a bonus!\n\n" +
            "PROGRESSION\n" +
            "──────────────────────────────\n" +
            "Beat each opponent to unlock the next.\n" +
            "Opponents respawn — keep farming money!\n\n" +
            "MUSCLE MEMORY (shop upgrade)\n" +
            "──────────────────────────────\n" +
            "• Adds passive bonus to every punch\n" +
            "• Earns money while idle in lobby\n" +
            "• Simulates fights while fully offline\n" +
            "• Auto-fights when you go idle in a fight";

        JTextArea ta = new JTextArea(msg);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
        ta.setBackground(new Color(0x1a1a2e));
        ta.setForeground(Color.WHITE);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(480, 400));
        JOptionPane.showMessageDialog(parent, sp, "🥊 HOW TO PLAY", JOptionPane.PLAIN_MESSAGE);
    }

    private void showInstructions() { showInstructions(this); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JLabel hudLabel(String text, Color color, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, size));
        l.setForeground(color);
        return l;
    }

    private JButton makeStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // Expose resolveEnding for ResultScreen
    public static int resolveEnding(float money, float fame) {
        boolean hiMoney = money >= GameConstants.MONEY_HIGH_THRESHOLD;
        boolean hiFame  = fame  >= GameConstants.FAME_HIGH_THRESHOLD;
        if (hiMoney && hiFame)   return 0; // legend
        if (!hiMoney && !hiFame) return 1; // forgotten
        if (hiMoney)             return 2; // rich
        return 3;                           // beloved
    }
}
