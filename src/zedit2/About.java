package zedit2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class About {
    private StringBuilder aboutBuilder;
    private static final int STARS = 30;
    private double[] sx = new double[STARS];
    private double[] vx = new double[STARS];
    private double[] sy = new double[STARS];
    private double[] vy = new double[STARS];
    private char[] sc = new char[STARS];
    private Random rng = new Random();
    private final char[] starChars = {'.', 'z', '*', '+', 'z', 't', ',', '☼', '.', '.', '☺', '☻'};
    private static final String about =
            "                                        \n" +
            "                                        \n" +
            "                   █  ▀    █    ▄▀▀▄    \n" +
            "    █▀▀█  ▄▀▀▄  ▄▀▀█  █  ▀▀█▀▀  ▀  █    \n" +
            "      ▄▀  █  █  █  █  █    █       █    \n" +
            "     ▄▀   █▀▀▀  █  █  █    █    ▄▀▀     \n" +
            "    ▄▀    █     █  █  █    █    █       \n" +
            "    █▄▄█  ▀▄▄▄  ▀▄▄▀  █    █    █▄▄█    \n" +
            "                                        \n" +
            "                                        \n" +
            "   (c) Mahou Shoujo ☼ Magical Moestar   \n" +
            "                                        \n" +
            "                                        ";

    private void initStars() {


        for (int i = 0; i < STARS; i++) {
            double x = rng.nextDouble() * 40.0;
            double y = rng.nextDouble() * 13.0;
            sx[i] = x;
            sy[i] = y;
            vx[i] = rng.nextDouble() * 0.56 + 0.14;
            vy[i] = rng.nextDouble() * 0.32 + 0.08;
            sc[i] = starChars[rng.nextInt(starChars.length)];
        }
    }

    public About(WorldEditor editor) {
        var dialog = new JDialog();
        dialog.setResizable(false);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setUndecorated(true);
        var ta = new JTextArea(13, 40);
        ta.setFocusable(false);

        aboutBuilder = new StringBuilder();
        initStars();
        var timer = new Timer(10, new ActionListener() {
            private float t = 0.0f;
            @Override
            public void actionPerformed(ActionEvent e) {
                t = (t + 0.01f) % 3;
                float r = 0.0f, g = 0.0f, b = 0.0f;
                if (t < 1.0f) {
                    var c = t;
                    g = 1.0f - c;
                    b = 1.0f;
                    r = c;
                } else if (t < 2.0f) {
                    var c = t - 1;
                    r = 1.0f;
                    b = 1.0f - c;
                    g = c;
                } else {
                    var c = t - 2;
                    g = 1.0f;
                    r = 1.0f - c;
                    b = c;
                }
                ta.setForeground(new Color(r, g, b));

                aboutBuilder.replace(0, aboutBuilder.length(), about);
                String ver = "v" + Main.VERSION;
                int verPos = 8 * 41 + 4;
                aboutBuilder.replace(verPos, verPos + ver.length(), ver);
                for (int i = 0; i < STARS; i++) {
                    sx[i] = (sx[i] + vx[i]);
                    sy[i] = (sy[i] + vy[i]);
                    if (sx[i] >= 40 || sy[i] >= 13) {
                        sx[i] %= 40;
                        sy[i] %= 13;
                        vx[i] = rng.nextDouble() * 0.56 + 0.14;
                        vy[i] = rng.nextDouble() * 0.32 + 0.08;
                        sc[i] = starChars[rng.nextInt(starChars.length)];
                    }
                    int pos = (int)sx[i] + (int)sy[i] * 41;
                    if (aboutBuilder.charAt(pos) == ' ') {
                        aboutBuilder.setCharAt(pos, sc[i]);
                    }
                }

                ta.setText(aboutBuilder.toString());
            }
        });
        timer.setRepeats(true);
        timer.start();
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                timer.stop();
            }
        });

        //ta.setText(about);

        dialog.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                dialog.dispose();
            }
        });
        ta.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dialog.dispose();
            }
        });

        ta.setFont(CP437.getFont());
        ta.setBackground(Color.BLACK);
        ta.setForeground(Color.GREEN);
        //ta.setCaretColor(Color.WHITE);
        ta.setEditable(false);
        dialog.add(ta);
        dialog.setIconImage(null);
        dialog.setTitle("About ZEdit2");

        dialog.pack();
        dialog.setLocationRelativeTo(editor.getFrameForRelativePositioning());
        dialog.setVisible(true);
    }
}
