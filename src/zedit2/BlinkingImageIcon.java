package zedit2;

import javax.swing.*;
import java.awt.*;

public class BlinkingImageIcon extends ImageIcon {
    Image img1, img2;
    public BlinkingImageIcon(Image img1, Image img2) {
        super(img1);
        this.img1 = img1;
        this.img2 = img2;
    }

    public void blink(boolean blinkState) {
        if (!blinkState) {
            this.setImage(img1);
        } else {
            this.setImage(img2);
        }
    }
}
