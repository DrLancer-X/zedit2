package zedit2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.WeakHashMap;

public class DosCanvas extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    public static final int CHAR_W = 8;
    public static final int CHAR_H = 14;
    public static final int CHAR_COUNT = 256;
    public static final int PALETTE_SIZE = 16;
    private static final int TRANSPARENT = 0x00000000;

    private byte[] charset;
    private byte[] paletteData;
    private int[] palette;
    private int charBufferHash;
    private static WeakHashMap<Integer, BufferedImage> charBufferCache = new WeakHashMap<>();
    private BufferedImage charBuffer;
    private int width = 0, height = 0;
    private BufferedImage[] boardBuffers = new BufferedImage[2];

    private byte[] chars;
    private byte[] cols;
    private double zoomx;
    private double zoomy;
    private boolean blink = true;
    private boolean blinkState = false;
    private int mouseState = 0;

    private int cursorX = 0, cursorY = 0;
    private int[] indicateX = null, indicateY = null;
    private int mouseCursorX = -1, mouseCursorY = -1;
    private int blockStartX = -1, blockStartY = -1;
    private int placingBlockW, placingBlockH;
    private boolean drawing, textEntry;

    private int boardW, boardH;
    private Atlas atlas;

    private WorldEditor editor;
    private GlobalEditor globalEditor;
    private boolean drawAtlasLines;

    private MouseEvent lastMouseEvent;
    private byte[] show;

    public DosCanvas(WorldEditor editor, double initialZoom) throws IOException {
        super();
        this.editor = editor;
        this.globalEditor = editor.getGlobalEditor();
        this.zoomx = initialZoom;
        this.zoomy = initialZoom;

        initialiseBuffers();

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    public void setBlinkMode(boolean state) {
        if (blink != state) {
            blink = state;
            refreshBuffer();
            resetData();
        }
    }

    private void initialiseBuffers() throws IOException {
        loadCharset(null);
        loadPalette(null);
        refreshBuffer();
    }

    private void refreshBuffer() {
        int[] hashes = new int[]{Arrays.hashCode(palette), Arrays.hashCode(charset)};
        charBufferHash = Arrays.hashCode(hashes);

        if (charBufferCache.containsKey(charBufferHash)) {
            charBuffer = charBufferCache.get(charBufferHash);
        } else {
            generateCharBuffer();
            charBufferCache.put(charBufferHash, charBuffer);
        }
    }

    private void generateCharBuffer() {

        int w = CHAR_W * CHAR_COUNT;
        int h = CHAR_H * PALETTE_SIZE;
        charBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        var raster = charBuffer.getRaster();
        var ar = new int[w * h];

        for (int col = 0; col < PALETTE_SIZE; col++) {
            for (int chr = 0; chr < CHAR_COUNT; chr++) {
                for (int y = 0; y < CHAR_H; y++) {
                    for (int x = 0; x < CHAR_W; x++) {
                        int px = chr * CHAR_W + x;
                        int py = col * CHAR_H + y;
                        int p = py * w + px;

                        // Is this pixel set in this char?
                        byte charRow = charset[chr * CHAR_H + y];
                        byte charMask = (byte)(128 >> x);
                        if ((charRow & charMask) != 0) {
                            ar[p] = palette[col];
                        } else {
                            ar[p] = TRANSPARENT;
                        }
                    }
                }
            }
        }

        raster.setDataElements(0, 0, w, h, ar);
    }

    private void loadPalette(File path) throws IOException {
        if (path != null) {
            paletteData = Files.readAllBytes(path.toPath());
            if (paletteData.length != 16 * 3) {
                throw new RuntimeException("Invalid palette size");
            }
        } else {
            paletteData = Data.DEFAULT_PALETTE;
        }
        palette = new int[PALETTE_SIZE];
        for (int i = 0; i < PALETTE_SIZE; i++) {
            int r = paletteData[i * 3 + 0] * 255 / 63;
            int g = paletteData[i * 3 + 1] * 255 / 63;
            int b = paletteData[i * 3 + 2] * 255 / 63;
            int color = 0xFF000000;
            color |= r << 16;
            color |= g << 8;
            color |= b;
            palette[i] = color;
        }
    }

    private void loadCharset(File path) throws IOException {
        if (path != null) {
            byte[] charsetBytes = Files.readAllBytes(path.toPath());
            if (charsetBytes.length == CHAR_COUNT * CHAR_H) {
                charset = charsetBytes;
            } else if (charsetBytes.length == 5027) {
                charset = Arrays.copyOfRange(charsetBytes, 1442, 5026);
            }
        } else {
            charset = Data.DEFAULT_CHARSET;
        }
        if (charset.length != CHAR_COUNT * CHAR_H) {
            throw new RuntimeException("Invalid charset size");
        }
    }

    public Dimension getPreferredSize() {
        int w = width;
        int h = height;
        if (w == 0 || h == 0) {
            w = 60;
            h = 25;
        }

        return new Dimension((int)(Math.round(w * CHAR_W * zoomx)),
                (int)(Math.round(h * CHAR_H * zoomy)));
    }

    public void setDimensions(int w, int h)
    {
        if (width != w || height != h) {
            width = w;
            height = h;
            fullRefresh();
        }
    }

    private void fullRefresh() {
        this.cols = new byte[width * height];
        this.chars = new byte[width * height];
        this.show = new byte[width * height];

        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = env.getDefaultScreenDevice();
        GraphicsConfiguration config = device.getDefaultConfiguration();

        for (int i = 0; i < 2; i++) {
            boardBuffers[i] = config.createCompatibleImage(width * CHAR_W, height * CHAR_H);
            //boardBuffers[i] = new BufferedImage(w * CHAR_W, h * CHAR_H, BufferedImage.TYPE_INT_RGB);
        }
    }

    public void setZoom(double zoomx, double zoomy) {
        this.zoomx = zoomx;
        this.zoomy = zoomy;
    }
    public void setData(int w, int h, byte[] cols, byte[] chars, int offsetX, int offsetY, int showing, byte[] show) {
        boolean redrawAll = false;
        setData(w, h, cols, chars, offsetX, offsetY, redrawAll, show);
    }
    public void setData(int w, int h, byte[] cols, byte[] chars, int offsetX, int offsetY, boolean redrawAll, byte[] show)
    {
        if (cols.length != w * h) throw new RuntimeException("Dimensions do not match colour array size");
        if (chars.length != w * h) throw new RuntimeException("Dimensions do not match char array size");

        Graphics[] boardBufferGraphics = new Graphics[2];
        for (int i = 0; i < 2; i++) {
            boardBufferGraphics[i] = boardBuffers[i].getGraphics();
        }
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int dpos = dy * w + dx;
                int x = dx + offsetX;
                int y = dy + offsetY;
                int pos = y * width + x;

                byte tshow;
                if (show != null) {
                    tshow = show[dpos];
                } else {
                    tshow = 0;
                }
                if (redrawAll || this.cols[pos] != cols[dpos] || this.chars[pos] != chars[dpos] || this.show[pos] != tshow) {
                    this.chars[pos] = chars[dpos];
                    this.cols[pos] = cols[dpos];
                    this.show[pos] = tshow;
                    int chr = chars[dpos] & 0xFF;
                    int col = cols[dpos] & 0xFF;

                    drawTile(boardBufferGraphics[0], chr, col, x, y, 1, 1, false);
                    if (tshow != 0) {
                        drawShow(boardBufferGraphics[1], chr, col, x, y, 1, 1, tshow);
                    } else {
                        drawTile(boardBufferGraphics[1], chr, col, x, y, 1, 1, true);
                    }
                }
            }
        }

        //System.arraycopy(cols, 0, this.cols, 0, cols.length);
        //System.arraycopy(chars, 0, this.chars, 0, chars.length);
    }

    public BufferedImage getBoardBuffer(boolean doubleWidth) {
        if (!doubleWidth)
            return boardBuffers[0];
        int w = boardBuffers[0].getWidth() * 2;
        int h = boardBuffers[0].getHeight();
        var tmpBuffer = boardBuffers[0].getScaledInstance(w, h, Image.SCALE_REPLICATE);
        var newBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        newBuffer.getGraphics().drawImage(tmpBuffer, 0, 0, null);
        return newBuffer;
    }

    private void drawShow(Graphics g, int chr, int col, int x, int y, int zoomx, int zoomy, byte show) {
        int chrs[] = {' ', '#', '!', 178, 15, col, '#'};
        int rcol = col;
        if (rcol == 0x00) rcol = 0x80;
        else if (rcol % 16 == rcol / 16) rcol = rcol & 0xF0;
        int cols[] = {rcol, rcol, rcol, rcol, rcol, 7, rcol};
        if (chr == chrs[show]) {
            chrs[show] = ' ';
        }
        drawTile(g, chrs[show], cols[show], x, y, zoomx, zoomy, false,false);
    }

    private void drawTile(Graphics g, int chr, int col, int x, int y, int zoomx, int zoomy, boolean blinkingTime) {
        drawTile(g, chr, col, x, y, zoomx, zoomy, blink, blinkingTime);
    }
    private void drawTile(Graphics g, int chr, int col, int x, int y, int zoomx, int zoomy, boolean blink, boolean blinkingTime) {
        if (blink && col >= 128) {
            // If blinking is on, for cXY and X >= 8, colours alternate between c(X-8)Y and c(X-8)(X-8)
            int bg = ((col & 0xF0) >> 4) - 8;
            int fg = col & 0x0F;
            if (!blinkingTime) {
                col = (bg << 4) | fg;
            } else {
                col = (bg << 4) | bg;
            }
        }
        int sx1 = chr * CHAR_W;
        int sy1 = (col & 0x0F) * CHAR_H;
        int sx2 = sx1 + CHAR_W;
        int sy2 = sy1 + CHAR_H;
        int dx1 = x * CHAR_W * zoomx;
        int dy1 = y * CHAR_H * zoomy;
        int dx2 = dx1 + CHAR_W * zoomx;
        int dy2 = dy1 + CHAR_H * zoomy;
        var bgColor = new Color(palette[(col & 0xF0) >> 4], true);

        g.drawImage(charBuffer, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgColor, null);
    }

    /**
     * Format of pattern is:
     * @ - character
     * $ - character, but no blink
     *   - blank
     * _ - col (bg)
     * # - col (fg)
     * >255 - 0xAABB (A = chr, B = col)

     */
    public BufferedImage extractCharImage(int chr, int col, int zoomx, int zoomy, boolean blinkingTime, String pattern) {
        return extractCharImageWH(chr, col, zoomx, zoomy, blinkingTime, pattern, pattern.length(), 1);
    }
    public BufferedImage extractCharImageWH(int chr, int col, int zoomx, int zoomy, boolean blinkingTime, String pattern, int w, int h) {
        var img = new BufferedImage(CHAR_W * zoomx * w, CHAR_H * zoomy * h, BufferedImage.TYPE_INT_ARGB);
        var blinkSaved = blink;
        for (int i = 0; i < pattern.length(); i++) {
            var c = pattern.charAt(i);
            switch (c) {
                case '@':
                    drawTile(img.getGraphics(), chr, col, i % w, i / w, zoomx, zoomy, blinkingTime);
                    break;
                case '$':
                    blink = false;
                    drawTile(img.getGraphics(), chr, col, i % w, i / w, zoomx, zoomy, blinkingTime);
                    blink = blinkSaved;
                    break;
                case '_':
                    blink = false;
                    drawTile(img.getGraphics(), 32, col, i % w, i / w, zoomx, zoomy, blinkingTime);
                    blink = blinkSaved;
                    break;
                case '#':
                    blink = false;
                    drawTile(img.getGraphics(), 254, col, i % w, i / w, zoomx, zoomy, blinkingTime);
                    blink = blinkSaved;
                    break;
                case ' ':
                    break;
                default:
                    int cVal = c;
                    blink = false;
                    drawTile(img.getGraphics(), (cVal & 0xFF00) >> 8, cVal & 0xFF, i % w, i / w, zoomx, zoomy, blinkingTime);
                    blink = blinkSaved;
                    break;
            }
        }
        return img;
    }

    private int tileX(int x) {
        return (int)(Math.round(x * CHAR_W * zoomx));
    }
    private int tileY(int y) {
        return (int)(Math.round(y * CHAR_H * zoomy));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        /*
        var mc = getMouseCoords(getMousePosition());
        if (mc != null) {
            mouseCursorX = mc.x;
            mouseCursorY = mc.y;
            if (mouseCursorX < 0 || mouseCursorY < 0 || mouseCursorX >= width || mouseCursorY >= height) {
                mouseCursorX = -1;
                mouseCursorY = -1;
            }
        }
        */
        g.setColor(new Color(0x7F7F7F));
        g.fillRect(0, 0, getWidth(), getHeight());

        if (g instanceof Graphics2D) {
            Object interpMode;

            // Select an appropriate rendering mode
            var xerror = Math.abs(Math.round(zoomx) - zoomx);
            var yerror = Math.abs(Math.round(zoomy) - zoomy);
            var error = Math.max(xerror, yerror);
            if (error < 0.001) {
                interpMode = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
            } else {
                if (xerror < 0.001 || yerror < 0.001) {
                    interpMode = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                } else {
                    interpMode = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                }
            }

            var g2 = (Graphics2D) g;
            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    interpMode);
            g2.setRenderingHints(rh);
        }

        int blinkImage = blinkState ? 1 : 0;
        drawImg(g, boardBuffers[blinkImage]);

        if (atlas != null && drawAtlasLines) {
            var lcBad = new Color(1.0f, 0.4f, 0.2f);
            var lcGood = new Color(0.2f, 1.0f, 0.4f, 0.5f);
            var lcNormal = new Color(1.0f, 1.0f, 1.0f);
            var lcVoid = new Color(1.0f, 1.0f, 1.0f, 0.2f);
            var gridW = atlas.getW();
            var gridH = atlas.getH();
            var grid = atlas.getGrid();
            var boards = editor.getBoards();
            int boardPixelW = tileX(boardW);
            int boardPixelH = tileY(boardH);
            final int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
            final int[][] walls_thick = {{0, 0, boardPixelW, 2},
                    {0, boardPixelH - 2, boardPixelW, 2},
                    {0, 0, 2, boardPixelH},
                    {boardPixelW - 2, 0, 2, boardPixelH}};
            final int[][] walls_thin = {{0, 0, boardPixelW, 1},
                    {0, boardPixelH - 1, boardPixelW, 1},
                    {0, 0, 1, boardPixelH},
                    {boardPixelW - 1, 0, 1, boardPixelH}};

            for (int y = 0; y < gridH; y++) {
                int py = tileY(y * boardH);
                for (int x = 0; x < gridW; x++) {
                    int px = tileX(x * boardW);
                    var boardIdx = grid[y][x];
                    if (boardIdx != -1) {
                        var board = boards.get(boardIdx);

                        for (int exit = 0; exit < 4; exit++) {
                            int exitd = board.getExit(exit);
                            int nx = x + dirs[exit][0];
                            int ny = y + dirs[exit][1];
                            var walls = walls_thick[exit];
                            if (exitd == 0) {
                                g.setColor(lcNormal);
                            } else {
                                g.setColor(lcBad);
                            }
                            if (nx >= 0 && ny >= 0 && nx < gridW && ny < gridH) {
                                if (exitd == grid[ny][nx]) {
                                    g.setColor(lcGood);
                                    walls = walls_thin[exit];
                                }
                            }
                            g.fillRect(walls[0] + px, walls[1] + py,
                                    walls[2], walls[3]);
                        }
                    } else {
                        g.setColor(lcVoid);
                        for (int exit = 0; exit < 4; exit++) {
                            var walls = walls_thin[exit];
                            g.fillRect(walls[0] + px, walls[1] + py,
                                    walls[2], walls[3]);
                        }
                    }

                    //g.drawLine(px, 0, px, ysize);
                    //g.drawLine(0, py, xsize, py);
                }
            }
        }
        //volatileBuffers[i] = config.createCompatibleVolatileImage(w * CHAR_W, h * CHAR_H);

        //var boardBuffer = boardBuffers[blinkState ? 1 : 0];


        if (drawing) {
            g.setColor(Color.GREEN);
        } else if (textEntry) {
            g.setColor(Color.YELLOW);
        } else {
            g.setColor(Color.LIGHT_GRAY);
        }

        g.draw3DRect(tileX(cursorX) - 1, tileY(cursorY) - 1, tileX(1) + 1, tileY(1) + 1, blinkState);

        if (mouseCursorX != -1 && editor.getFrame().isFocused()) {
            g.setColor(new Color(0x7FFFFFFF, true));
            g.drawRect(tileX(mouseCursorX) - 1, tileY(mouseCursorY) - 1,
                   tileX(1) + 1, tileY(1) + 1);
        }

        if (indicateX != null) {
            g.setColor(new Color(0x3399FF));
            for (int i = 0; i < indicateX.length; i++) {
                int x = indicateX[i];
                int y = indicateY[i];
                if (x == -1) continue;
                g.drawRect(tileX(x), tileY(y), tileX(1) - 1, tileY(1) - 1);
            }
        }

        if (blockStartX != -1) {
            g.setColor(new Color(0x7F3399FF, true));
            int x = Math.min(cursorX, blockStartX);
            int y = Math.min(cursorY, blockStartY);
            int w = (Math.max(cursorX, blockStartX) - x + 1);
            int h = (Math.max(cursorY, blockStartY) - y + 1);
            g.fillRect(tileX(x), tileY(y), tileX(w), tileY(h));
        }

        if (globalEditor.isBlockBuffer()) {
            g.setColor(new Color(0x5FFF8133, true));
            int x2 = Math.min(width - 1, cursorX + globalEditor.getBlockBufferW() - 1);
            int y2 = Math.min(height - 1, cursorY + globalEditor.getBlockBufferH() - 1);
            int w = x2 - cursorX + 1;
            int h = y2 - cursorY + 1;
            g.fillRect(tileX(cursorX), tileY(cursorY), tileX(w), tileY(h));
        }

        if (placingBlockW != -1) {
            g.setColor(new Color(0x5F33ff99, true));
            int x2 = Math.min(width - 1, cursorX + placingBlockW - 1);
            int y2 = Math.min(height - 1, cursorY + placingBlockH - 1);
            int w = x2 - cursorX + 1;
            int h = y2 - cursorY + 1;
            g.fillRect(tileX(cursorX), tileY(cursorY), tileX(w), tileY(h));
        }
        //g.drawImage(charBuffer, 0, 0, new Color(palette[7], true), null);
        //g.drawImage(charBuffer, 0, 0, 16, 14, 8, 14, 16, 28, bgColor, null);
    }

    private void drawImg(Graphics g, Image image) {
        g.drawImage(image, 0, 0, tileX(width), tileY(height), 0, 0, width * CHAR_W, height * CHAR_H, null);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        mouseMoveCommon(e);
    }

    private Point getMouseCoords(Point p) {
        if (p == null) { return null; }
        int x = (int)(p.x / zoomx / CHAR_W);
        int y = (int)(p.y / zoomy / CHAR_H);
        return new Point(x, y);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseMoveCommon(e, getButton(e));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseMoveCommon(e, 0);
    }

    private int getButton(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e))
            return 1;
        else if (SwingUtilities.isRightMouseButton(e))
            return 2;
        else if (SwingUtilities.isMiddleMouseButton(e))
            return 3;
        else
            return 0;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseMoveCommon(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (mouseCursorX != -1) {
            mouseCursorX = -1;
            mouseCursorY = -1;
            repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoveCommon(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseMoveCommon(e);
    }

    private void mouseMoveCommon(MouseEvent e) {
        mouseMoveCommon(e, mouseState);
    }
    private void mouseMoveCommon(MouseEvent e, int heldDown) {
        lastMouseEvent = e;
        mouseState = heldDown;
        editor.mouseMotion(e, heldDown);
        int newMouseCursorX, newMouseCursorY;
        var mc = getMouseCoords(e.getPoint());
        int x = mc.x;
        int y = mc.y;
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) {
            newMouseCursorX = -1;
            newMouseCursorY = -1;
        } else {
            newMouseCursorX = x;
            newMouseCursorY = y;
        }
        if (newMouseCursorX != mouseCursorX || newMouseCursorY != mouseCursorY) {
            mouseCursorX = newMouseCursorX;
            mouseCursorY = newMouseCursorY;
            repaint();
        }
    }

    public void setBlink(boolean b) {
        blinkState = b;
        repaint();
    }

    public void setCursor(int x, int y) {
        cursorX = x;
        cursorY = y;
        repaint();
    }

    public void setIndicate(int[] x, int[] y) {
        indicateX = x;
        indicateY = y;
        repaint();
    }

    public String htmlColour(int colIdx) {
        int r = (palette[colIdx] & 0x00FF0000) >> 16;
        int g = (palette[colIdx] & 0x0000FF00) >> 8;
        int b = (palette[colIdx] & 0x000000FF) >> 0;
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public double getZoomX() {
        return zoomx;
    }
    public double getZoomY() {
        return zoomy;
    }

    public void setSelectionBlock(int blockStartX, int blockStartY) {
        this.blockStartX = blockStartX;
        this.blockStartY = blockStartY;
        repaint();
    }

    public void setPlacingBlock(int w, int h) {
        this.placingBlockW = w;
        this.placingBlockH = h;
        repaint();
    }

    public void setTextEntry(boolean state) {
        textEntry = state;
        repaint();
    }

    public void setDrawing(boolean state) {
        drawing = state;
        repaint();
    }

    public int getCharW(int x) {
        return getCharW(x, zoomx);
    }
    public int getCharH(int y) {
        return getCharH(y, zoomy);
    }
    public int getCharW(int x, double zoomx) {
        return (int)Math.round(CHAR_W * x * zoomx);
    }
    public int toCharX(int x) {
        return (int)(1.0 * x / CHAR_W / zoomx);
    }
    public int getCharH(int y, double zoomy) {
        return (int)Math.round(CHAR_H * y * zoomy);
    }
    public int toCharY(int y) {
        return (int)(1.0 * y / CHAR_H / zoomy);
    }

    // TODO: HOT FUNCTION (4%)
    public void drawVoid(int x, int y, int w, int h) {
        for (int i = 0; i < 2; i++) {
            var graphics = boardBuffers[i].getGraphics();
            graphics.setColor(new Color(0x7F7F7F));
            graphics.fillRect(x * CHAR_W, y * CHAR_H, w * CHAR_W, h * CHAR_H);
        }
    }

    public void setPalette(File file) throws IOException {
        loadPalette(file);
        refreshBuffer();
        resetData();
        repaint();
    }

    public void setCharset(File file) throws IOException {
        loadCharset(file);
        refreshBuffer();
        resetData();
        repaint();
    }

    public byte[] getCharset() {
        return charset;
    }
    public byte[] getPalette() {
        return paletteData;
    }

    public void setCP(File charset, File palette) throws IOException {
        loadCharset(charset);
        loadPalette(palette);
        refreshBuffer();
        resetData();
        repaint();
    }

    private void resetData() {
        if (cols != null && chars != null) {
            setData(width, height, cols, chars, 0, 0, true, show);
        }
    }

    public void setAtlas(Atlas atlas, int boardW, int boardH, boolean drawAtlasLines) {
        this.atlas = atlas;
        this.boardW = boardW;
        this.boardH = boardH;
        this.drawAtlasLines = drawAtlasLines;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() < 0) {
            editor.wheelUp(e);
        } else {
            editor.wheelDown(e);
        }
    }

    public void recheckMouse() {
        if (lastMouseEvent != null) {
            // TODO
            // TODO
            // TODO
            // TODO
            // TODO
        }
    }
}
