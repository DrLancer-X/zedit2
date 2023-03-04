package zedit2;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class BufferModel extends AbstractListModel<String> implements ListCellRenderer<String> {
    private BufferManager manager;
    private WorldEditor editor;
    private int size;
    private int selected;
    private String prefix;
    ArrayList<ListDataListener> listeners = new ArrayList<>();
    public BufferModel(BufferManager manager, WorldEditor editor) {
        prefix = editor.prefix();
        this.editor = editor;
        this.manager = manager;
        selected = editor.getGlobalEditor().getCurrentBufferNum();
        updateBuffer();
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public String getElementAt(int index) {
        int bufferNum = idxToBufNum(index);
        var key = String.format(prefix+"BUF_%d", bufferNum);
        var ge = editor.getGlobalEditor();
        if (ge.isKey(key)) {
            return ge.getString(key);
        } else {
            return null;
        }
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        int bufferNum = idxToBufNum(index);
        boolean isSel = bufferNum == selected;
        final int BUFFER_W = 64, BUFFER_H = 64;

        BufferedImage img = new BufferedImage(BUFFER_W, BUFFER_H, BufferedImage.TYPE_INT_RGB);
        var g = img.getGraphics();
        int yOff = g.getFontMetrics().getAscent();
        g.setColor(new Color(0x7F7F7F));
        g.fillRect(0, 0, BUFFER_W, BUFFER_H);

        var bufImg = getClipImage(editor.getCanvas(), value);
        if (bufImg != null) {
            int imgW = bufImg.getWidth();
            int imgH = bufImg.getHeight();
            int scale = 0;
            if (imgW * 4 <= BUFFER_W && imgH * 4 <= BUFFER_H) scale = 4;
            else if (imgW * 3 <= BUFFER_W && imgH * 3 <= BUFFER_H) scale = 3;
            else if (imgW * 2 <= BUFFER_W && imgH * 2 <= BUFFER_H) scale = 2;
            else if (imgW <= BUFFER_W && imgH <= BUFFER_H) scale = 1;

            int scaleW, scaleH;

            if (scale == 0) {
                int imgL = Math.max(imgW, imgH);
                double factor = 1.0 * BUFFER_W / imgL;
                scaleW = (int)(Math.round(imgW * factor));
                scaleH = (int)(Math.round(imgH * factor));
            } else {
                scaleW = imgW * scale;
                scaleH = imgH * scale;
            }
            int offsetX = (BUFFER_W - scaleW) / 2;
            int offsetY = (BUFFER_H - scaleH) / 2;

            g.drawImage(bufImg, offsetX, offsetY, offsetX + scaleW, offsetY + scaleH, 0, 0, imgW, imgH, null);
        }

        if (isSel) {
            g.setColor(Color.BLACK);
            g.drawRect(1, 1, BUFFER_W - 3, BUFFER_H - 3);
            g.setColor(Color.RED);
            g.drawRect(2, 2, BUFFER_W - 5, BUFFER_H - 5);
            g.setColor(Color.YELLOW);
            g.drawRect(3, 3, BUFFER_W - 7, BUFFER_H - 7);
            g.setColor(Color.RED);
            g.drawRect(4, 4, BUFFER_W - 9, BUFFER_H - 9);
            g.setColor(Color.BLACK);
            g.drawRect(5, 5, BUFFER_W - 11, BUFFER_H - 11);
        }
        if (isSelected) {
            g.setColor(new Color(0x7F5FFFFF, true));
            g.drawRect(0, 0, BUFFER_W - 1, BUFFER_H - 1);
        }

        if (bufferNum < 10) {
            g.setColor(Color.BLACK);
            g.drawString(String.valueOf(bufferNum), 1 + 1, yOff - 2 + 1);
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(bufferNum), 1, yOff - 2);
        }


        return new JLabel(new ImageIcon(img));
        //return new JLabel(String.format("%s%d%s", isSel ? "*" : "", bufferNum, containsSomething ? " X" : ""));
    }

    public static BufferedImage getClipImage(DosCanvas canvas, String value) {
        if (value != null) {
            var clip = Clip.decode(value);
            int w = clip.getW();
            int h = clip.getH();
            var tiles = clip.getTiles();
            BufferBoard bb = new BufferBoard(clip.isSzzt(), w, h);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    bb.setTile(x, y, tiles[y * w + x]);
                }
            }
            byte[] chars = new byte[1];
            byte[] cols = new byte[1];
            StringBuilder gfx = new StringBuilder();
            //canvas.extractCharImage()
            int recCol = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    bb.drawCharacter(cols, chars, 0, x, y);
                    int c = ((chars[0] & 0xFF) << 8) | (cols[0] & 0xFF);
                    if (chars[0] == 0) {
                        recCol = cols[0] & 0xFF;
                        c = '$';
                    }
                    gfx.append((char) c);
                }
            }
            return canvas.extractCharImageWH(0, recCol,
                    1, 1, false, gfx.toString(), w, h);
        } else {
            return null;
        }
    }

    public void updateBuffer() {
        var ge = editor.getGlobalEditor();
        int bufMax = ge.getInt(prefix + "BUF_MAX", 0);
        if (ge.isKey(prefix+"BUF_0") && bufMax == 0) bufMax = 9;
        else if (bufMax > 0) bufMax = bufNumToIdx(bufMax);
        bufMax = (bufMax + 10) / 5 * 5;
        int oldSize = size;
        size = Math.max(bufMax, 10);
        if (size > oldSize) {
            updateListeners(ListDataEvent.INTERVAL_ADDED, oldSize, size - 1);
        } else if (size < oldSize) {
            updateListeners(ListDataEvent.INTERVAL_REMOVED, size, oldSize - 1);
        }
        if (size != oldSize) {
            manager.resizeList();
        }
    }

    public void updateBuffer(int bufferNum) {
        bufferNum = bufNumToIdx(bufferNum);
        boolean withinOldRange = bufferNum < size;
        updateBuffer();
        if (withinOldRange) {
            updateListeners(ListDataEvent.CONTENTS_CHANGED, bufferNum, bufferNum);
        }
    }

    private void updateListeners(int event, int first, int last) {
        for (var listener : listeners) {
            ListDataEvent e = new ListDataEvent(this, event, first, last);
            listener.contentsChanged(e);
        }
    }

    public void updateSelected(int num) {
        int oldSelected = selected;
        selected = num;
        if (oldSelected >= 0) updateBuffer(oldSelected);
        if (selected >= 0) updateBuffer(selected);
    }

    public int bufNumToIdx(int num) {
        if (num == 0) return 9;
        else if (num <= 9) return num - 1;
        else return num;
    }
    public int idxToBufNum(int idx) {
        if (idx < 9) return idx + 1;
        else if (idx == 9) return 0;
        else return idx;
    }

    public void remove(int idx) {
        //System.out.printf("remove(%d)\n", idx);
        if (idx < 0) return;
        int bufNum = idxToBufNum(idx);
        String key = String.format(prefix+"BUF_%d", bufNum);
        var ge = editor.getGlobalEditor();
        int bufMax = ge.getInt(prefix+"BUF_MAX", 0);
        if (selected == bufNum) {
            ge.clearBufferSelected();
            selected = -1;
        }
        editor.getGlobalEditor().removeKey(key);
        if (bufNum == bufMax && bufMax > 0) {
            while (bufMax > 0) {
                bufMax--;
                if (ge.isKey(String.format(prefix+"BUF_%d", bufMax))) break;
            }
            ge.setInt(prefix+"BUF_MAX", bufMax);
        }
        updateBuffer(bufNum);
    }

    public boolean add(int dropIndex, int fromIndex, String data) {
        throw new UnsupportedOperationException("Insert not supported");
    }

    public boolean set(int dropIndex, int fromIndex, String data) {
        //System.out.printf("set(%d, %d, %s)\n", dropIndex, fromIndex, data);
        if (dropIndex == fromIndex) return false;
        if (dropIndex < 0) return false;
        int bufNum = idxToBufNum(dropIndex);
        String key = String.format(prefix+"BUF_%d", bufNum);
        var ge = editor.getGlobalEditor();
        int bufMax = ge.getInt(prefix+"BUF_MAX", 0);
        if (bufNum > bufMax) {
            ge.setInt(prefix+"BUF_MAX", bufNum);
        }
        String encodedBuffer = BufferManager.getBufferDataString(data);
        ge.setString(key, encodedBuffer);
        updateBuffer(bufNum);
        return true;
    }
}
