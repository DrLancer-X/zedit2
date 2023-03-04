package zedit2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ColourSelector extends JPanel implements KeyActionReceiver, MouseListener {
    ActionListener listener;
    Image colourPalette;
    int zoomx = 2, zoomy = 2;
    int borderx = 8, bordery = 8;
    int x, y;
    int width, height;
    JDialog dialog;
    DosCanvas canvas;
    int selectorMode;
    boolean wrap = false;

    public static final int COLOUR = 0;
    public static final int TEXTCOLOUR = 1;
    public static final int CHAR = 2;
    private static String[] TITLES = {"Select a colour", "Select a text colour", "Select a character"};

    public ColourSelector(GlobalEditor ge, int col, JDialog outerDialog, ActionListener listener, DosCanvas canvas, int selectorMode) {
        this.canvas = canvas;
        this.listener = listener;
        this.addMouseListener(this);
        this.selectorMode = selectorMode;
        if (selectorMode == CHAR) wrap = true;
        dialog = outerDialog;

        switch (this.selectorMode) {
            case COLOUR:
            case TEXTCOLOUR:
                width = 16;
                height = 16;
                break;
            case CHAR:
                width = 32;
                height = 8;
                break;
            default:
                throw new RuntimeException("Invalid colour selector mode");
        }

        x = col % width;
        y = col / width;
        colourPalette = canvas.extractCharImageWH(0, 0, zoomx, zoomy, false, getColourPattern(), width, height);
        Util.addKeybind(ge, this, this, "Up");
        Util.addKeybind(ge, this, this, "Down");
        Util.addKeybind(ge, this, this, "Left");
        Util.addKeybind(ge, this, this, "Right");
        Util.addKeybind(ge, this, this, "Home");
        Util.addKeybind(ge, this, this, "End");

        Util.addKeybind(ge, this, this, "Enter");
        Util.addKeybind(ge, this, this, "Enter");
        Util.addKeybind(ge, this, this, "Escape");

        if (selectorMode == CHAR) {
            dialog.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    int c = e.getKeyChar();
                    if (c >= 32 && c <= 127) {
                        x = c % width;
                        y = c / width;
                        upd();
                    }
                }
            });
        }

        upd();
    }

    public static void createColourSelector(WorldEditor editor, int col, Component relativeTo, ActionListener listener, int selectorMode) {
        createColourSelector(editor, col, relativeTo, null, listener, selectorMode);
    }
    public static void createColourSelector(WorldEditor editor, int col, Component relativeTo, Object owner, ActionListener listener, int selectorMode) {
        JDialog colourSelectorDialog;
        if (owner != null) {
            if (owner instanceof Dialog) colourSelectorDialog = new JDialog((Dialog)owner);
            else if (owner instanceof Frame) colourSelectorDialog = new JDialog((Frame)owner);
            else if (owner instanceof Window) colourSelectorDialog = new JDialog((Window)owner);
            else throw new RuntimeException("invalid owner");
        } else {
            colourSelectorDialog = new JDialog();
        }
        colourSelectorDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        colourSelectorDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        colourSelectorDialog.setResizable(false);
        var ge = editor.getGlobalEditor();
        var cs = new ColourSelector(ge, col, colourSelectorDialog, listener, editor.getCanvas(), selectorMode);
        colourSelectorDialog.getContentPane().add(cs);
        colourSelectorDialog.pack();
        colourSelectorDialog.setLocationRelativeTo(relativeTo);
        colourSelectorDialog.setVisible(true);
    }

    @Override
    public void keyAction(String actionName, ActionEvent e) {
        switch (actionName) {
            case "Up":
                operationCursorMove(0, -1);
                break;
            case "Down":
                operationCursorMove(0, 1);
                break;
            case "Left":
                operationCursorMove(-1, 0);
                break;
            case "Right":
                operationCursorMove(1, 0);
                break;
            case "Home":
                operationCursorMove(-999, -999);
                break;
            case "End":
                operationCursorMove(999, 999);
                break;
            case "Enter": case "Space":
                operationSubmit();
                break;
            case "Escape":
                operationExit();
                break;
        }
    }

    private void operationCursorMove(int xOff, int yOff) {
        int xMin = selectorMode == TEXTCOLOUR ? width - 1 : 0;
        if (x == 0 && xOff == -1 && wrap) {
            x = width - 1;
            y = (y + height - 1) % height;
        } else if (x == width - 1 && xOff == 1 && wrap) {
            x = 0;
            y = (y + 1) % height;
        } else {
            x = Util.clamp(x + xOff, xMin, width - 1);
            y = Util.clamp(y + yOff, 0, height - 1);
        }
        upd();
    }



    private void operationSubmit() {
        listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, String.valueOf(getCol())));
        operationExit();
    }

    private void operationExit() {
        dialog.dispose();
    }

    private void upd() {
        dialog.setTitle(TITLES[selectorMode] + ": " + getCol());
        if (selectorMode != CHAR) {
            dialog.setIconImage(canvas.extractCharImage(254, getCol(), 2, 2, false, "$"));
        } else {
            dialog.setIconImage(canvas.extractCharImage(getCol(), 0x1b, 2, 2, false, "$"));
        }
        repaint();
    }

    private int getCol() {
        return y * width + x;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(DosCanvas.CHAR_W * zoomx * width + borderx * 2, DosCanvas.CHAR_H * zoomy * height + bordery * 2);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(0x7F7F7F));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.drawImage(colourPalette, borderx, bordery, null);


        int cursorX = x * DosCanvas.CHAR_W * zoomx + 8;
        int cursorY = y * DosCanvas.CHAR_H * zoomy + 8;
        int cursorW = DosCanvas.CHAR_W * zoomx;
        int cursorH = DosCanvas.CHAR_H * zoomy;
        g.setColor(Color.BLUE);
        g.fillRect(cursorX - 6, cursorY - 6, cursorW + 12, cursorH + 12);
        g.setColor(Color.WHITE);
        g.fillRect(cursorX - 4, cursorY - 4, cursorW + 8, cursorH + 8);
        g.setColor(Color.BLUE);
        g.fillRect(cursorX - 2, cursorY - 2, cursorW + 4, cursorH + 4);

        g.drawImage(colourPalette, cursorX, cursorY, cursorX + cursorW, cursorY + cursorH,
                cursorX - 8, cursorY - 8, cursorX + cursorW - 8, cursorY + cursorH - 8, null);
    }

    private String getColourPattern() {
        var sb = new StringBuilder(256);
        for (int col = 0; col < 256; col++) {
            int c = col;
            if (selectorMode == TEXTCOLOUR && (col & 0x0F) != 15) c = 0;
            if (selectorMode != CHAR) {
                sb.append((char) ((254 << 8) | c));
            } else {
                sb.append((char) ((c << 8) | 0x8F));
            }
        }
        return sb.toString();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (mouseSelectColour(e))
            operationSubmit();
    }

    private boolean mouseSelectColour(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        if (mouseX < 8 || mouseY < 8) return false;
        mouseX = (mouseX - 8) / zoomx / DosCanvas.CHAR_W;
        mouseY = (mouseY - 8) / zoomy / DosCanvas.CHAR_H;
        if (mouseX >= width || mouseY >= height) return false;
        x = mouseX;
        y = mouseY;
        upd();
        return true;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseSelectColour(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }
}
