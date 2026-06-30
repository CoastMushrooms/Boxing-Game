package com.boxergame;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ResultScreen extends JPanel {

    public interface ResultListener { void onContinue(); }

    private static final int CHAMPION_INDEX = EnemyRoster.CHAMPIONSHIP_PATH.length - 1;

    private static final Color BG = new Color(0x0D0D1A);
    private static final Color GOLD = new Color(0xFFD700);

    public ResultScreen(boolean won, float moneyDelta, float fameDelta,
                        int fightIndex, ResultListener listener) {
        setBackground(BG);
        setLayout(new BorderLayout());

        GameState s = GameState.get();
        boolean firstChampionWin = won && (fightIndex == CHAMPION_INDEX);

        String title, detail, btnText;
        String endingArtName = null;

        if (firstChampionWin) {
            int ending = LobbyScreen.resolveEnding(s.getMoney(), s.getFame());
            title = endingTitle(ending);
            detail = endingBody(ending, s.getMoney(), s.getFame()) + "\n\nThe fight continues — keep earning!";
            btnText = "Keep Fighting!";
            endingArtName = endingArtResource(ending);
        } else if (won) {
            title = "VICTORY!";
            detail = "+" + (int) moneyDelta + " earned\n+" + (int) fameDelta + " fame\n\nEnemy respawns — go earn more!";
            btnText = "Back to Lobby";
        } else {
            title = "KNOCKED OUT";
            detail = "You lost $" + (int) Math.abs(moneyDelta) + "\nTrain harder and try again.";
            btnText = "Back to Lobby";
        }

        JLabel tvTitle = new JLabel(title, SwingConstants.CENTER);
        tvTitle.setFont(new Font("SansSerif", Font.BOLD, 36));
        tvTitle.setForeground(GOLD);
        tvTitle.setBorder(new EmptyBorder(30, 20, 16, 20));

        JLabel ivEnding = new JLabel();
        ivEnding.setHorizontalAlignment(SwingConstants.CENTER);
        if (endingArtName != null) {
            BufferedImage img = Assets.getImage(endingArtName, 220, 220);
            if (img != null) ivEnding.setIcon(new ImageIcon(img));
        }

        JTextArea tvDetail = new JTextArea(detail);
        tvDetail.setEditable(false);
        tvDetail.setFont(new Font("SansSerif", Font.PLAIN, 16));
        tvDetail.setForeground(new Color(0xCCCCCC));
        tvDetail.setBackground(BG);
        tvDetail.setLineWrap(true);
        tvDetail.setWrapStyleWord(true);
        tvDetail.setAlignmentX(CENTER_ALIGNMENT);
        tvDetail.setBorder(new EmptyBorder(0, 40, 30, 40));

        JButton btnContinue = new JButton(btnText);
        btnContinue.setBackground(new Color(0xE53935));
        btnContinue.setForeground(Color.WHITE);
        btnContinue.setFont(new Font("SansSerif", Font.BOLD, 18));
        btnContinue.setFocusPainted(false);
        btnContinue.setBorderPainted(false);
        btnContinue.setOpaque(true);
        btnContinue.setPreferredSize(new Dimension(300, 56));
        btnContinue.addActionListener(e -> listener.onContinue());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(BG);
        btnPanel.setBorder(new EmptyBorder(0, 0, 30, 0));
        btnPanel.add(btnContinue);

        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setBackground(BG);
        if (endingArtName != null) centre.add(ivEnding);
        centre.add(tvDetail);

        add(tvTitle, BorderLayout.NORTH);
        add(centre,  BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private String endingTitle(int e) {
        switch (e) {
            case 0: return "LEGEND RETIRES";
            case 1: return "FORGOTTEN FIGHTER";
            case 2: return "RICH BUT FORGOTTEN";
            case 3: return "BELOVED BUT BROKE";
            default: return "RETIREMENT";
        }
    }

    private String endingBody(int e, float money, float fame) {
        String stats = "\n\nFinal Money: $" + (int)money + "\nFame: " + (int)fame;
        switch (e) {
            case 0: return "You rode off into the sunset with both the riches and the roar of the crowd. A true legend." + stats;
            case 1: return "The fight was long, but neither fortune nor fame came your way. Perhaps next time." + stats;
            case 2: return "You walk away loaded, but nobody remembers your name. Money doesn't always buy legacy." + stats;
            case 3: return "The fans love you, but your pockets are empty. You gave everything for the sport." + stats;
            default: return stats;
        }
    }

    private String endingArtResource(int e) {
        switch (e) {
            case 0: return "ending_legend";
            case 1: return "ending_forgotten";
            case 2: return "ending_rich";
            case 3: return "ending_beloved";
            default: return "ending_legend";
        }
    }
}
