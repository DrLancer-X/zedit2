package zedit2;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

// From https://stackoverflow.com/questions/10136794/limiting-the-number-of-characters-in-a-jtextfield
public class LimitDocFilter extends DocumentFilter {
    private int maxChars;
    public LimitDocFilter(int maxChars) {
        this.maxChars = maxChars;
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        int currentLength = fb.getDocument().getLength();
        int overLimit = (currentLength + text.length()) - maxChars - length;
        if (overLimit > 0) {
            text = text.substring(0, text.length() - overLimit);
        }
        if (text.length() > 0) {
            super.replace(fb, offset, length, text, attrs);
        }
    }
}
