package zedit2;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

public class CP437 {
    private static String unicodeString = "\u0000☺☻♥♦♣♠•◘○◙♂♀♪♫☼►◄↕‼¶§▬↨↑↓→←∟↔▲▼ !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~⌂ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿⌐¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤⌠⌡÷≈°\u2219\u00B7√ⁿ²■\u00A0";
    private static Font font = null;
    private static HashMap<Character, Character> reverse = null;

    private static void buildReverseTable() {
        if (reverse == null) {
            reverse = new HashMap<>();
            for (int i = 0; i < 256; i++) {
                char dos = (char) i;
                char uni = unicodeString.charAt(i);
                reverse.put(uni, dos);
            }
        }
    }

    public static Font getFont() {
        if (font != null) return font;
        try {
            //Main.class.getClassLoader().getResource
            //new File("Px437_IBM_EGA8.ttf")
            var u = Main.class.getClassLoader().getResourceAsStream("Px437_IBM_EGA8.ttf");
            font = Font.createFont(Font.TRUETYPE_FONT, u).deriveFont(16f);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            font = new Font(Font.MONOSPACED, Font.PLAIN, 11);
        }
        return font;
    }

    public static void registerFont() {
        var font = getFont();
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
    }

    public static String toUnicode(byte[] input, boolean drawCr)
    {
        buildReverseTable();

        StringBuilder output = new StringBuilder();
        for (byte b : input) {
            int c = b & 0xFF;
            if (c == 13 && !drawCr) {
                output.append('\n');
            } else {
                output.append(unicodeString.charAt(c));
            }
        }
        return output.toString();
    }

    public static String toUnicode(byte[] input)
    {
        return toUnicode(input, true);
    }

    public static byte[] toBytes(String unicode, boolean drawCr) {
        buildReverseTable();
        byte[] output = new byte[unicode.length()];
        for (int i = 0; i < output.length; i++) {
            int c = unicode.charAt(i);
            if (c == '\n' && !drawCr) {
                c = '\r';
            } else if (reverse.containsKey((char)c)) {
                c = reverse.get((char)c);
            }
            if (c < 256) {
                output[i] = (byte) c;
            } else {
                output[i] = (byte)'?';
            }
        }
        return output;
    }

    public static byte[] toBytes(String unicode) {
        return toBytes(unicode, true);
    }
}
