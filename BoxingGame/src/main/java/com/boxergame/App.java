package com.boxergame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class App {

    private static final String CARD_LOBBY = "lobby";
    private static final String CARD_FIGHT = "fight";
    private static final String CARD_SHOP = "shop";
    private static final String CARD_RESULT = "result";

    private JFrame       mFrame;
    private JPanel       mCards;
    private CardLayout   mCardLayout;
    private SaveManager  mSave;
    private LobbyScreen  mLobby;

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new App().launch());
    }

    private void launch() {
        mFrame = new JFrame("Last Season");
        mFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mFrame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onExit(); }
        });

        mSave = new SaveManager();
        mCardLayout = new CardLayout();
        mCards = new JPanel(mCardLayout);
        mCards.setBackground(Color.BLACK);

        SoundManager lobbySound = new SoundManager();
        mLobby = new LobbyScreen(mSave, lobbySound, new LobbyScreen.LobbyListener() {
            public void onFightSelected(int idx) { App.this.onFightSelected(idx); }
            public void onShopClicked()           { App.this.onShopClicked(); }
        });
        mCards.add(mLobby, CARD_LOBBY);

        mFrame.setContentPane(mCards);
        mFrame.setPreferredSize(new Dimension(480, 760));
        mFrame.pack();
        mFrame.setLocationRelativeTo(null);
        mFrame.setVisible(true);

        int champProgress = mSave.load();
        mLobby.setChampionshipProgress(champProgress);
        showLobby();
        checkOfflineEarnings();

        if (!mSave.hasShownInstructions()) {
            mSave.setShownInstructions();
            SwingUtilities.invokeLater(() -> LobbyScreen.showInstructions(mFrame));
        }
    }

    private void showLobby() {
        mLobby.onResume();
        mCardLayout.show(mCards, CARD_LOBBY);
        mFrame.requestFocusInWindow();
    }

    private void onFightSelected(int fightIndex) {
        mLobby.onPause();

        EnemyData enemy = fightIndex < EnemyRoster.CHAMPIONSHIP_PATH.length
                ? EnemyRoster.CHAMPIONSHIP_PATH[fightIndex]
                : EnemyRoster.OPTIONAL_FIGHTS[fightIndex - EnemyRoster.CHAMPIONSHIP_PATH.length];

        FightScreen fightScreen = new FightScreen(enemy, fightIndex,
                (won, moneyDelta, fameDelta, idx) -> onFightFinished(won, moneyDelta, fameDelta, idx));

        removeCardByType(FightScreen.class);
        mCards.add(fightScreen, CARD_FIGHT);
        mCardLayout.show(mCards, CARD_FIGHT);
        fightScreen.requestFocusInWindow();
    }

    private void onFightFinished(boolean won, float moneyDelta, float fameDelta, int fightIndex) {
        int progress = mSave.load();
        mSave.save(progress);

        ResultScreen result = new ResultScreen(won, moneyDelta, fameDelta, fightIndex,
                () -> onResultContinue());
        removeCardByType(ResultScreen.class);
        mCards.add(result, CARD_RESULT);
        mCardLayout.show(mCards, CARD_RESULT);
    }

    private void onResultContinue() {
        removeCardByType(ResultScreen.class);
        removeCardByType(FightScreen.class);
        int progress = mSave.load();
        mLobby.setChampionshipProgress(progress);
        mLobby.buildFightList();
        showLobby();
    }

    private void onShopClicked() {
        mLobby.onPause();
        ShopScreen shop = new ShopScreen(mSave, () -> {
            removeCardByType(ShopScreen.class);
            showLobby();
        });
        removeCardByType(ShopScreen.class);
        mCards.add(shop, CARD_SHOP);
        mCardLayout.show(mCards, CARD_SHOP);
    }

    private void removeCardByType(Class<?> type) {
        for (Component c : mCards.getComponents()) {
            if (type.isInstance(c)) { mCards.remove(c); break; }
        }
    }

    private void checkOfflineEarnings() {
        long lastMs = mSave.getLastOnlineMs();
        if (!GameState.get().muscleMemoryUnlocked.get() || lastMs <= 0) return;
        IdleEngine idle = new IdleEngine();
        IdleEngine.OfflineResult result = idle.calculateOfflineEarnings(lastMs);
        idle.destroy();
        if (result != null) {
            mLobby.refreshHud();
            JOptionPane.showMessageDialog(mFrame, result.summary(), "Welcome Back!", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onExit() {
        mLobby.onPause();
        mLobby.destroy();
        mFrame.dispose();
        System.exit(0);
    }
}
