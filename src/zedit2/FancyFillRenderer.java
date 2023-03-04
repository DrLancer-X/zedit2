package zedit2;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class FancyFillRenderer implements ListCellRenderer<String> {
    private final WorldEditor editor;
    private static int WIDTH = 96;
    private static int HEIGHT = 14;
    private Color bg;

    public FancyFillRenderer(WorldEditor editor) {
        this.editor = editor;
        bg = this.editor.getFrame().getBackground();
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        Image img = BufferModel.getClipImage(editor.getCanvas(), value);
        if (img == null) {
            return new JLabel("(not selected)");
        }
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        if (w > WIDTH) {
            double scale = 1.0 * w / WIDTH;
            //int scaleInt = (int) Math.ceil(scale);
            int nh = (int) (Math.round(h * scale));
            var dupImg = new BufferedImage(w, nh, BufferedImage.TYPE_INT_RGB);
            var g = dupImg.getGraphics();
            int yoff = (nh % HEIGHT) / 2;
            for (int y = 0; y < nh; y += HEIGHT) {
                g.drawImage(img, 0, y - yoff, null);
            }
            var scaledDup = dupImg.getScaledInstance(WIDTH, -1, Image.SCALE_SMOOTH);
            img = scaledDup;

            //img = img.getScaledInstance(WIDTH, -1, Image.SCALE_SMOOTH).
        } else {
            var dupImg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            var g = dupImg.getGraphics();
            g.setColor(bg);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.drawImage(img, (WIDTH - w) / 2, 0, null);
            img = dupImg;
        }
        JLabel label = new JLabel(new ImageIcon(img));
        label.setOpaque(true);
        label.setBackground(bg);
        return label;
    }
}
