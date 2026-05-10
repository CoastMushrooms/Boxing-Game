package com.boxergame;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/** Desktop replacement for ShopActivity. */
public class ShopScreen extends JPanel {

    public interface ShopListener { void onBackClicked(); }

    private static final int[] GLOVES_COSTS = { 800, 2000, 4500 };
    private static final int[] SHOES_COSTS  = { 600, 1500, 3500 };
    private static final int[] ARMOR_COSTS  = { 900, 2200, 5000 };

    private final GameState   mState = GameState.get();
    private final SaveManager mSave;
    private final ShopListener mListener;

    private JLabel mTvMoney;
    private JLabel mTvGloves, mTvShoes, mTvArmor, mTvMuscle;
    private JButton mBtnGloves, mBtnShoes, mBtnArmor, mBtnMuscle;

    private static final Color BG       = new Color(0x1A1A2E);
    private static final Color PANEL    = new Color(0x16213E);
    private static final Color GOLD     = new Color(0xFFD700);
    private static final Color BTN_CLR  = new Color(0xE53935);
    private static final Color BTN_GOLD = new Color(0xB8860B);

    public ShopScreen(SaveManager save, ShopListener listener) {
        mSave     = save;
        mListener = listener;
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        refreshAll();
    }

    private void buildUI() {
        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(PANEL);
        titleBar.setBorder(new EmptyBorder(8, 12, 8, 12));

        JButton btnBack = styledBtn("← Back", BTN_CLR);
        btnBack.addActionListener(e -> mListener.onBackClicked());
        JLabel title = new JLabel("🛒  EQUIPMENT SHOP", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(GOLD);

        mTvMoney = new JLabel("Balance: $0", SwingConstants.CENTER);
        mTvMoney.setFont(new Font("SansSerif", Font.PLAIN, 16));
        mTvMoney.setForeground(Color.WHITE);

        titleBar.add(btnBack,   BorderLayout.WEST);
        titleBar.add(title,     BorderLayout.CENTER);
        titleBar.add(mTvMoney,  BorderLayout.SOUTH);

        // Items list
        JPanel items = new JPanel();
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setBackground(BG);
        items.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Gloves card
        mTvGloves  = itemLabel("");
        mBtnGloves = styledBtn("UPGRADE", BTN_CLR);
        mBtnGloves.addActionListener(e -> buyGloves());
        items.add(makeItemCard(Assets.getImage("item_gloves", 48, 48),
                               mTvGloves, mBtnGloves, PANEL));
        items.add(Box.createVerticalStrut(10));

        // Shoes card
        mTvShoes  = itemLabel("");
        mBtnShoes = styledBtn("UPGRADE", BTN_CLR);
        mBtnShoes.addActionListener(e -> buyShoes());
        items.add(makeItemCard(Assets.getImage("item_shoes", 48, 48),
                               mTvShoes, mBtnShoes, PANEL));
        items.add(Box.createVerticalStrut(10));

        // Armor card
        mTvArmor  = itemLabel("");
        mBtnArmor = styledBtn("UPGRADE", BTN_CLR);
        mBtnArmor.addActionListener(e -> buyArmor());
        items.add(makeItemCard(Assets.getImage("item_armor", 48, 48),
                               mTvArmor, mBtnArmor, PANEL));
        items.add(Box.createVerticalStrut(10));

        // Muscle Memory card (gold)
        mTvMuscle  = itemLabel("");
        mBtnMuscle = styledBtn("BUY MUSCLE MEMORY", BTN_GOLD);
        mBtnMuscle.addActionListener(e -> buyMuscle());
        items.add(makeItemCard(Assets.getImage("item_muscle", 48, 48),
                               mTvMuscle, mBtnMuscle, new Color(0x1a1500)));
        items.add(Box.createVerticalStrut(10));

        JScrollPane scroll = new JScrollPane(items);
        scroll.setBackground(BG);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);

        add(titleBar, BorderLayout.NORTH);
        add(scroll,   BorderLayout.CENTER);
    }

    private JPanel makeItemCard(java.awt.image.BufferedImage img, JLabel info,
                                 JButton btn, Color bg) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(bg);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(0x2a2a4a), 1, true),
            new EmptyBorder(12, 14, 12, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        if (img != null) {
            JLabel icon = new JLabel(new ImageIcon(img));
            icon.setPreferredSize(new Dimension(54, 54));
            card.add(icon, BorderLayout.WEST);
        }
        card.add(info, BorderLayout.CENTER);
        card.add(btn,  BorderLayout.EAST);
        return card;
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    private void refreshAll() {
        mTvMoney.setText("Balance: $" + (int) mState.getMoney());

        int gl = mState.getGlovesLevel();
        int sh = mState.getShoesLevel();
        int ar = mState.getArmorLevel();

        mTvGloves.setText("<html><b>Power Gloves  Lv " + gl + "/" + GLOVES_COSTS.length + "</b><br>"
            + "+25% damage, +15% earnings per level<br>"
            + "Damage ×" + String.format("%.2f", mState.getDamageMult()) + "<br>"
            + "Cost: " + (gl < GLOVES_COSTS.length ? "$" + GLOVES_COSTS[gl] : "MAX") + "</html>");
        mBtnGloves.setText(gl < GLOVES_COSTS.length ? "UPGRADE  $" + GLOVES_COSTS[gl] : "MAXED ✅");
        mBtnGloves.setEnabled(gl < GLOVES_COSTS.length);

        mTvShoes.setText("<html><b>Speed Shoes  Lv " + sh + "/" + SHOES_COSTS.length + "</b><br>"
            + "+20% earnings per level<br>"
            + "Earn ×" + String.format("%.2f", mState.getEarnMult()) + "<br>"
            + "Cost: " + (sh < SHOES_COSTS.length ? "$" + SHOES_COSTS[sh] : "MAX") + "</html>");
        mBtnShoes.setText(sh < SHOES_COSTS.length ? "UPGRADE  $" + SHOES_COSTS[sh] : "MAXED ✅");
        mBtnShoes.setEnabled(sh < SHOES_COSTS.length);

        int defPct = Math.round((1f - mState.getDefenseMult()) * 100);
        mTvArmor.setText("<html><b>Iron Vest  Lv " + ar + "/" + ARMOR_COSTS.length + "</b><br>"
            + "-15% damage taken per level<br>"
            + "Damage reduced: " + defPct + "%<br>"
            + "Cost: " + (ar < ARMOR_COSTS.length ? "$" + ARMOR_COSTS[ar] : "MAX") + "</html>");
        mBtnArmor.setText(ar < ARMOR_COSTS.length ? "UPGRADE  $" + ARMOR_COSTS[ar] : "MAXED ✅");
        mBtnArmor.setEnabled(ar < ARMOR_COSTS.length);

        boolean muscle = mState.muscleMemoryUnlocked.get();
        mTvMuscle.setText("<html><b>Muscle Memory</b><br>"
            + (muscle ? "✅ OWNED — earns $ passively while you're away"
                      : "Auto-earn idle engine. Earns money even offline.<br>Cost: $" + GameConstants.MUSCLE_MEMORY_COST) + "</html>");
        mBtnMuscle.setText(muscle ? "OWNED ✅" : "BUY  $" + GameConstants.MUSCLE_MEMORY_COST);
        mBtnMuscle.setEnabled(!muscle);
    }

    // ── Purchases ─────────────────────────────────────────────────────────────

    private void buyGloves() {
        int lvl = mState.getGlovesLevel();
        if (lvl >= GLOVES_COSTS.length) { toast("Already maxed!"); return; }
        if (mState.spendMoney(GLOVES_COSTS[lvl])) {
            mState.upgradeGloves(); toast("Power Gloves → Lv " + mState.getGlovesLevel() + "!"); save();
        } else toast("Need $" + GLOVES_COSTS[lvl]);
        refreshAll();
    }

    private void buyShoes() {
        int lvl = mState.getShoesLevel();
        if (lvl >= SHOES_COSTS.length) { toast("Already maxed!"); return; }
        if (mState.spendMoney(SHOES_COSTS[lvl])) {
            mState.upgradeShoes(); toast("Speed Shoes → Lv " + mState.getShoesLevel() + "!"); save();
        } else toast("Need $" + SHOES_COSTS[lvl]);
        refreshAll();
    }

    private void buyArmor() {
        int lvl = mState.getArmorLevel();
        if (lvl >= ARMOR_COSTS.length) { toast("Already maxed!"); return; }
        if (mState.spendMoney(ARMOR_COSTS[lvl])) {
            mState.upgradeArmor(); toast("Iron Vest → Lv " + mState.getArmorLevel() + "!"); save();
        } else toast("Need $" + ARMOR_COSTS[lvl]);
        refreshAll();
    }

    private void buyMuscle() {
        if (mState.muscleMemoryUnlocked.get()) { toast("Already owned!"); return; }
        if (mState.spendMoney(GameConstants.MUSCLE_MEMORY_COST)) {
            mState.muscleMemoryUnlocked.set(true);
            toast("Muscle Memory unlocked! You'll earn passively."); save();
        } else toast("Need $" + GameConstants.MUSCLE_MEMORY_COST);
        refreshAll();
    }

    private void save() { mState.saveState(); }

    private void toast(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Shop", JOptionPane.INFORMATION_MESSAGE);
    }

    private JLabel itemLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(new Color(0xEEEEEE));
        return l;
    }

    private JButton styledBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(160, 44));
        return b;
    }
}
