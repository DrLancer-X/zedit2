package zedit2;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import static zedit2.DosCanvas.*;

public class Converter {
    private final boolean szzt;
    private final int outw;
    private final int outh;
    private final int mw;
    private final int mh;
    private final int maxObjs;
    private final BufferedImage scaledImage;
    private boolean blinking = true;

    private ArrayList<Integer> elementIdsList = new ArrayList<>();
    private ArrayList<Integer> elementCharsList = new ArrayList<>();
    private int[] elementIds;
    private int[] elementChars;
    private byte[] palette;
    private byte[] charset;
    private boolean[][] colChrCombos;
    private double[] charRmse;
    private double[] objRmse;
    private int[][] objChr;
    private int[][] objCol;

    private byte[][] charGfx;

    private boolean running = false;
    private Thread thread;
    private ConverterCallback callback;
    private int checkVal;

    public Converter(boolean szzt, int outw, int outh, int mw, int mh, int maxObjs, BufferedImage scaledImage) {
        this.szzt = szzt;
        this.outw = outw;
        this.outh = outh;
        this.mw = mw;
        this.mh = mh;
        this.maxObjs = maxObjs;
        this.scaledImage = scaledImage.getSubimage(0, 0, scaledImage.getWidth(), scaledImage.getHeight());
    }

    public void addElement(int id, int chr) {
        elementIdsList.add(id);
        elementCharsList.add(chr);
    }

    public void beginConvert() {
        thread = new Thread() {
            @Override
            public void run() {
                convertThread();
            }
        };
        running = true;
        thread.start();
    }

    private void convertThread() {
        finaliseSetup();

        ArrayList<ArrayList<Integer>> coords = new ArrayList<>();
        for (int y = 0; y < outh; y++) {
            for (int x = 0; x < outw; x++) {
                coords.add(Util.pair(x, y));
            }
        }
        Consumer<? super ArrayList<Integer>> act = (Consumer<ArrayList<Integer>>) integers -> convChar(integers.get(0), integers.get(1));
        coords.parallelStream().forEach(act);

        if (maxObjs > 0) {
            PriorityQueue<ConvObj> best = new PriorityQueue<>();
            for (int y = 0; y < outh; y++) {
                for (int x = 0; x < outw; x++) {
                    int pos = y * outw + x;
                    double objImprovement = charRmse[pos] - objRmse[pos];
                    best.add(new ConvObj(objImprovement, x, y));
                }
            }
            for (int i = 0; i < maxObjs; i++) {
                if (!running) return;
                var conv = best.poll();
                if (conv == null) break;
                if (conv.getRmseImprovement() <= 0.0000) break;
                //System.out.printf("%dth best: rmse improvement %f\n", i, conv.getRmseImprovement());
                int x = conv.getX();
                int y = conv.getY();
                int id = ZType.OBJECT;
                int chr = objChr[y][x];
                int col = objCol[y][x];
                callback.converted(checkVal, x, y, id, col, chr, col);
            }
        }

        /*
        for (int y = 0; y < outh; y++) {
            for (int x = 0; x < outw; x++) {
                System.out.printf("%d / %d\n", y * outw + x + 1, outh * outw + 1);
                convChar(x, y);
                if (!running) return;
            }
        }
         */

        if (running) callback.finished(checkVal);
    }



    private void finaliseSetup() {
        // Write this into a proper array so we can access it faster

        elementIds = new int[elementIdsList.size()];
        elementChars = new int[elementCharsList.size()];
        for (int i = 0; i < elementIds.length; i++) {
            elementIds[i] = elementIdsList.get(i);
            elementChars[i] = elementCharsList.get(i);
        }

        // Reduce charset down to macroblocks
        int gfxW = CHAR_W / mw * CHAR_COUNT;
        int gfxH = CHAR_H / mh;
        charGfx = new byte[gfxH][gfxW];
        byte add = (byte) (112 / mw / mh);
        for (int row = 0; row < CHAR_H; row++) {
            for (int chr = 0; chr < CHAR_COUNT; chr++) {
                byte charByte = charset[chr * CHAR_H + row];
                for (int col = 0; col < CHAR_W; col++) {
                    byte mask = (byte) (128 >> col);
                    if ((charByte & mask) != 0) {
                        charGfx[row / mh][(chr * CHAR_W + col) / mw] += add;
                    }
                }
            }
        }

        // Allocate room for rmse storage
        charRmse = new double[outw * outh];
        objRmse = new double[outw * outh];
        objChr = new int[outh][outw];
        objCol = new int[outh][outw];

        // Generate col/chr combos
        int colRange = blinking ? 128 : 256;
        colChrCombos = new boolean[colRange][CHAR_COUNT];

        for (int i = 0; i < colRange; i++) {
            Arrays.fill(colChrCombos[i], true);
        }

    }

    private void convChar(int charX, int charY) {
        if (!running) return;
        int[] fullrgb = scaledImage.getRGB(charX * CHAR_W, charY * CHAR_H, CHAR_W, CHAR_H, null, 0, CHAR_W);
        double[][][] lab = new double[CHAR_H / mh][CHAR_W / mw][3];
        for (int y = 0; y < CHAR_H; y++) {
            for (int x = 0; x < CHAR_W; x++) {
                int rgb = fullrgb[y * CHAR_W + x];
                int r = (rgb & 0xFF0000) >> 16;
                int g = (rgb & 0x00FF00) >> 8;
                int b = rgb & 0x0000FF;
                var l = Util.rgbToLab(r, g, b);
                for (int c = 0; c < 3; c++) {
                    lab[y / mh][x / mw][c] += l[c];
                }
            }
        }
        double div = mw * mh;
        for (int y = 0; y < CHAR_H / mh; y++) {
            for (int x = 0; x < CHAR_W / mw; x++) {
                for (int c = 0; c < 3; c++) {
                    lab[y][x][c] /= div;
                }
            }
        }
        int colRange = blinking ? 128 : 256;
        double lowest = Double.MAX_VALUE;
        int bestChr = -1, bestId = -1, bestCol = -1, bestVCol = -1;
        var combos = colChrCombos.clone();

        for (int elementIdx = 0; elementIdx < elementChars.length; elementIdx++) {
            int elementId = elementIds[elementIdx];
            int elementChar = elementChars[elementIdx];

            int textColour = ZType.getTextColour(szzt, elementId);
            if (textColour == -1) {
                // Not text, so use the char
                for (int col = 0; col < (elementId == ZType.EMPTY ? 1 : colRange); col++) {
                    if (!running) return;
                    combos[col][elementChar] = false;
                    double rmse = cmpChr(elementChar, col, lab);
                    if (rmse < lowest) {
                        lowest = rmse;
                        bestId = elementId;
                        bestChr = elementChar;
                        bestCol = col;
                        bestVCol = col;
                    }
                }
            } else {
                // Text, so use the col
                if (textColour < colRange) {
                    for (int chr = 0; chr < CHAR_COUNT; chr++) {
                        if (!running) return;
                        combos[textColour][chr] = false;
                        double rmse = cmpChr(chr, textColour, lab);
                        if (rmse < lowest) {
                            lowest = rmse;
                            bestId = elementId;
                            bestChr = chr;
                            bestCol = chr;
                            bestVCol = textColour;
                        }
                    }
                }
            }
        }
        {
            final int chr = bestChr;
            final int col = bestCol;
            final int id = bestId;
            final int vcol = bestVCol;
            charRmse[charY * outw + charX] = lowest;
            if (!running) return;
            SwingUtilities.invokeLater(() -> {
                callback.converted(checkVal, charX, charY, id, col, chr, vcol);
            });
        }

        if (maxObjs > 0) {
            lowest = Double.MAX_VALUE;

            for (int col = 0; col < colRange; col++) {
                for (int chr = 0; chr < CHAR_COUNT; chr++) {
                    if (!running) return;
                    if (combos[col][chr]) {
                        double rmse = cmpChr(chr, col, lab);
                        if (rmse < lowest) {
                            lowest = rmse;
                            bestChr = chr;
                            bestCol = col;
                        }
                    }
                }
            }

            objRmse[charY * outw + charX] = lowest;
            objChr[charY][charX] = bestChr;
            objCol[charY][charX] = bestCol;
        }
    }

    private double[] palLab(int col) {
        int r = palette[col * 3 + 0];
        int g = palette[col * 3 + 1];
        int b = palette[col * 3 + 2];
        r = r * 255 / 63;
        g = g * 255 / 63;
        b = b * 255 / 63;
        return Util.rgbToLab(r, g, b);
    }

    private double cmpChr(int elementChar, int col, double[][][] lab) {
        var bg = palLab(col / 16);
        var fg = palLab(col % 16);
        int offX = elementChar * CHAR_W / mw;
        double rmse = 0.0;
        for (int y = 0; y < CHAR_H / mh; y++) {
            for (int x = 0; x < CHAR_W / mw; x++) {
                double fgMult = charGfx[y][x + offX] / 112.0;
                double bgMult = (112 - charGfx[y][x + offX]) / 112.0;
                for (int c = 0; c < 3; c++) {
                    double diff = bg[c] * bgMult + fg[c] * fgMult - lab[y][x][c];
                    rmse += diff * diff;
                }
            }
        }
        return rmse;
    }

    public void stop() {
        if (!running) return;
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            return;
        }
    }

    public void setGfx(DosCanvas canvas) {
        palette = canvas.getPalette().clone();
        charset = canvas.getCharset().clone();
    }

    public void setBlink(boolean blinking) {
        this.blinking = blinking;
    }

    public void setCallback(ConverterCallback converterCallback) {
        this.callback = converterCallback;
    }

    public void setCheckVal(int checkVal) {
        this.checkVal = checkVal;
    }
}
