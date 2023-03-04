package zedit2;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

public class EditingModePane extends JPanel {
    private String text = "";
    private Color col = Color.BLACK;
    private String override_text;
    private Color override_col;
    private Timer timer;
    private Timer override_timer;

    private int currentHash = -1;

    public EditingModePane() {
    }

    @Override
    public Dimension getPreferredSize() {
        int w = 128;
        int h = 24;

        return new Dimension(w, h);
    }

    public void display(Color col, String text) {
        int hash = col.hashCode() + text.hashCode();
        if (hash == currentHash) return;
        currentHash = hash;

        this.col = col;
        this.text = text;
        repaint();

        stopTimer();
    }

    public void display(Color[] cols, String text) {
        int hash = Arrays.hashCode(cols) + text.hashCode();
        if (hash == currentHash) return;
        currentHash = hash;

        this.col = cols[0];
        this.text = text;
        repaint();

        stopTimer();
        timer = new Timer(300, new ActionListener() {
            private int colNum = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                colNum++;
                col = cols[colNum % cols.length];
                repaint();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }

    public void display(Color col, int duration, String text) {
        override_text = text;
        override_col = col;
        final var started = LocalDateTime.now();

        if (override_timer != null) {
            override_timer.stop();
            override_timer = null;
        }

        override_timer = new Timer(10, e -> {
            var current = LocalDateTime.now();
            var dur = Duration.between(started, current);
            var ms = dur.toMillis();
            int leadIn = duration / 4;
            int leadOut = duration / 2;
            if (ms < leadIn) {
                override_col = mix(col, Color.WHITE, 1.0 * ms / leadIn);
            } else if (ms < leadOut) {
                override_col = mix(Color.WHITE, col, 1.0 * (ms - leadIn) / leadIn);
            } else if (ms < duration) {
                override_col = mix(col, Color.BLACK, 1.0 * (ms - leadOut) / leadOut);
            } else {
                override_text = null;
                override_col = null;
                override_timer.stop();
                override_timer = null;
            }
            repaint();
        });
        override_timer.setRepeats(true);
        override_timer.start();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        var col = getCol();
        var darkCol = mix(col, Color.BLACK, 0.5);
        var lightCol = mix(col, Color.WHITE, 0.5);

        g.setColor(darkCol);
        //drawBorder(g, 0, false);
        drawBorder(g, 2, false);
        drawBorder(g, 4, true);
        g.setColor(lightCol);
        Font[] fonts = {
                CP437.getFont(),
                new Font(Font.SANS_SERIF, Font.BOLD, 14),
                new Font(Font.SANS_SERIF, Font.BOLD, 12),
                new Font(Font.SANS_SERIF, Font.PLAIN, 12),
                new Font(Font.SANS_SERIF, Font.BOLD, 10),
                new Font(Font.SANS_SERIF, Font.PLAIN, 10),
                new Font(Font.SANS_SERIF, Font.BOLD, 8),
                new Font(Font.SANS_SERIF, Font.PLAIN, 8)
        };

        FontMetrics metrics;
        int textw, texth;
        int w = getWidth();
        int h = getHeight();
        var txt = getText();
        for (var font : fonts) {
            g.setFont(font);
            metrics = g.getFontMetrics();
            texth = metrics.getHeight();
            textw = metrics.stringWidth(txt);
            if (textw > w - 16) continue;

            int x = (w - textw) / 2;
            int y = (h - texth) / 2 + metrics.getAscent();

            g.drawString(txt, x, y);
            break;
        }

    }

    private String getText() {
        return override_text == null ? text : override_text;
    }

    private Color getCol() {
        return override_col == null ? col : override_col;
    }

    private static Color mix(Color col, Color mixWith, double mixFactor) {
        int r = (int) Math.round((col.getRed() - (mixFactor * col.getRed())) + mixWith.getRed() * mixFactor);
        int g = (int) Math.round((col.getGreen() - (mixFactor * col.getGreen())) + mixWith.getGreen() * mixFactor);
        int b = (int) Math.round((col.getBlue() - (mixFactor * col.getBlue())) + mixWith.getBlue() * mixFactor);
        return new Color(r, g, b);
    }

    private void drawBorder(Graphics g, int border, boolean fill) {
        int w = getWidth();
        int h = getHeight();
        if (fill) {
            g.fillRect(border, border, w - border * 2, h - border * 2);
        } else {
            g.drawRect(border, border, w - border * 2 - 1, h - border * 2 - 1);
        }
    }
}
