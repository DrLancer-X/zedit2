package zedit2;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class Util {

    // Unused
    // int charset[] = new int[] {0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007, 0x0008, 0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x000E, 0x000F, 0x0010, 0x0011, 0x0012, 0x0013, 0x0014, 0x0015, 0x0016, 0x0017, 0x0018, 0x0019, 0x001A, 0x001B, 0x001C, 0x001D, 0x001E, 0x001F, 0x0020, 0x0021, 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0027, 0x0028, 0x0029, 0x002A, 0x002B, 0x002C, 0x002D, 0x002E, 0x002F, 0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037, 0x0038, 0x0039, 0x003A, 0x003B, 0x003C, 0x003D, 0x003E, 0x003F, 0x0040, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047, 0x0048, 0x0049, 0x004A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F, 0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057, 0x0058, 0x0059, 0x005A, 0x005B, 0x005C, 0x005D, 0x005E, 0x005F, 0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067, 0x0068, 0x0069, 0x006A, 0x006B, 0x006C, 0x006D, 0x006E, 0x006F, 0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077, 0x0078, 0x0079, 0x007A, 0x007B, 0x007C, 0x007D, 0x007E, 0x007F, 0x00C7, 0x00FC, 0x00E9, 0x00E2, 0x00E4, 0x00E0, 0x00E5, 0x00E7, 0x00EA, 0x00EB, 0x00E8, 0x00EF, 0x00EE, 0x00EC, 0x00C4, 0x00C5, 0x00C9, 0x00E6, 0x00C6, 0x00F4, 0x00F6, 0x00F2, 0x00FB, 0x00F9, 0x00FF, 0x00D6, 0x00DC, 0x00A2, 0x00A3, 0x00A5, 0x20A7, 0x0192, 0x00E1, 0x00ED, 0x00F3, 0x00FA, 0x00F1, 0x00D1, 0x00AA, 0x00BA, 0x00BF, 0x2310, 0x00AC, 0x00BD, 0x00BC, 0x00A1, 0x00AB, 0x00BB, 0x2591, 0x2592, 0x2593, 0x2502, 0x2524, 0x2561, 0x2562, 0x2556, 0x2555, 0x2563, 0x2551, 0x2557, 0x255D, 0x255C, 0x255B, 0x2510, 0x2514, 0x2534, 0x252C, 0x251C, 0x2500, 0x253C, 0x255E, 0x255F, 0x255A, 0x2554, 0x2569, 0x2566, 0x2560, 0x2550, 0x256C, 0x2567, 0x2568, 0x2564, 0x2565, 0x2559, 0x2558, 0x2552, 0x2553, 0x256B, 0x256A, 0x2518, 0x250C, 0x2588, 0x2584, 0x258C, 0x2590, 0x2580, 0x03B1, 0x00DF, 0x0393, 0x03C0, 0x03A3, 0x03C3, 0x00B5, 0x03C4, 0x03A6, 0x0398, 0x03A9, 0x03B4, 0x221E, 0x03C6, 0x03B5, 0x2229, 0x2261, 0x00B1, 0x2265, 0x2264, 0x2320, 0x2321, 0x00F7, 0x2248, 0x00B0, 0x2219, 0x00B7, 0x221A, 0x207F, 0x00B2, 0x25A0, 0x00A0};
    /*
 ☺☻♥♦♣♠•◘○◙♂♀♪♫☼►◄↕‼¶§▬↨↑↓→←∟↔▲▼
 !"#$%&'()*+,-./0123456789:;<=>?
@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_
`abcdefghijklmnopqrstuvwxyz{|}~⌂
ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒ
áíóúñÑªº¿⌐¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐
└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀
αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤⌠⌡÷≈°∙⋅√ⁿ²■ 
    */

    public static byte[] readPascalString(byte inputLen, byte[] bytes, int from, int to) {
        int len = toUInt8(inputLen);
        if (to < from) throw new RuntimeException("Invalid range");

        int stringLength = to - from;
        if (len > stringLength) len = stringLength;

        var stringArray = Arrays.copyOfRange(bytes, from, from + len);
        return stringArray;
    }

    public static void writePascalString(byte[] str, byte[] bytes, int lenPos, int from, int to)
    {
        if (to < from) throw new RuntimeException("Invalid range");

        int bufferLength = to - from;

        int len = Math.min(bufferLength, str.length);
        if (len > 255) throw new RuntimeException("Range too large for Pascal string");

        System.arraycopy(str, 0, bytes, from, len);
        if (bufferLength > len) {
            Arrays.fill(bytes, from + len, to, (byte)0);
        }
        bytes[lenPos] = (byte)len;
    }

    public static int toUInt8(byte input)
    {
        return input & 0xFF;
    }

    public static int getInt16(byte[] bytes, int offset) {
        int val = toUInt8(bytes[offset]);
        val |= toUInt8(bytes[offset + 1]) << 8;
        if (val > 32767) {
            val = (short)(-65536 + val);
        }
        return val;
    }
    public static void setInt16(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte)(value & 0xFF);
        bytes[offset + 1] = (byte)(((short) value & 0xFF00) >> 8);
    }

    public static int getInt8(byte[] bytes, int offset) {
        return toUInt8(bytes[offset]);
    }
    public static void setInt8(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte)(value & 0xFF);
    }

    public static int clamp(int val, int min, int max) {
        return Math.max(Math.min(val, max), min);
    }
    public static double clamp(double val, double min, double max) {
        return Math.max(Math.min(val, max), min);
    }

    public static void setInt32(byte[] bytes, int offset, int value) {
        bytes[offset + 0] = (byte)((value & 0x000000FF) >> 0);
        bytes[offset + 1] = (byte)((value & 0x0000FF00) >> 8);
        bytes[offset + 2] = (byte)((value & 0x00FF0000) >> 16);
        bytes[offset + 3] = (byte)((value & 0xFF000000) >> 24);
    }

    public static int getInt32(byte[] bytes, int offset) {
        int val = toUInt8(bytes[offset]);
        val |= toUInt8(bytes[offset + 1]) << 8;
        val |= toUInt8(bytes[offset + 2]) << 16;
        val |= toUInt8(bytes[offset + 3]) << 24;
        return val;
    }

    public static KeyStroke getKeyStroke(GlobalEditor ge, String actionName) {
        var keyStroke = KeyStroke.getKeyStroke(ge.getString("K_" + actionName));
        return keyStroke;
    }

    public static void addKeybind(GlobalEditor ge, KeyActionReceiver receiver, JComponent component, String actionName) {
        if (actionName.isBlank()) return;
        var keyStroke = getKeyStroke(ge, actionName);
        if (keyStroke == null) return;
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
        if ((keyStroke.getModifiers() | InputEvent.ALT_DOWN_MASK) != 0) {
            var altgrMod = keyStroke.getModifiers() | InputEvent.ALT_GRAPH_DOWN_MASK;
            var altgrKeyStroke = KeyStroke.getKeyStroke(keyStroke.getKeyCode(), altgrMod);
            component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(altgrKeyStroke, actionName);
        }
        component.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                receiver.keyAction(actionName, e);
            }
        });
    }

    /*
    public static void disableWarning() {
        // From https://stackoverflow.com/questions/46454995/how-to-hide-warning-illegal-reflective-access-in-java-9-without-jvm-argument
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
    }

    public static void removeAltProcessor() {
        disableWarning();
        // From https://stackoverflow.com/questions/56339708/disable-single-alt-type-to-activate-the-menu
        try {
            Method method = KeyboardFocusManager.class.getDeclaredMethod("getKeyEventPostProcessors");
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<KeyEventPostProcessor> list =
                    (java.util.List<KeyEventPostProcessor>) method.invoke(KeyboardFocusManager.getCurrentKeyboardFocusManager());
            for (KeyEventPostProcessor pp : list) {
                if (pp.getClass().getName().contains("WindowsRootPaneUI")) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(pp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     */

    public static int getUInt16(byte[] bytes, int offset) {
        return getInt16(bytes, offset) & 0xFFFF;
    }

    public static File getJarPath() {
        try {
            return new File(Util.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        } catch (URISyntaxException e) {
            return new File(".");
        }
    }


    public static String getExtensionless(String path) {
        var p = Path.of(path);
        var baseName = p.getFileName().toString();
        var extPos = baseName.lastIndexOf('.');
        if (extPos == -1) return baseName;
        return baseName.substring(0, extPos);
    }

    public static String keyStrokeString(KeyStroke keyStroke) {
        if (keyStroke == null) return null;
        var mods = keyStroke.getModifiers();
        boolean shift = (mods & InputEvent.SHIFT_DOWN_MASK) != 0;
        boolean ctrl = (mods & InputEvent.CTRL_DOWN_MASK) != 0;
        boolean alt = (mods & InputEvent.ALT_DOWN_MASK) != 0;
        String keyName;

        switch (keyStroke.getKeyCode()) {
            default:
                keyName = KeyStroke.getKeyStroke(keyStroke.getKeyCode(), 0).toString().replace("pressed ","");
                break;
        }
        return String.format("%s%s%s%s",
                shift ? "Shift+" : "",
                ctrl ? "Ctrl+" : "",
                alt ? "Alt+" : "",
                keyName);
    }

    public static void setKeyStroke(GlobalEditor ge, String actionName, KeyStroke keyStroke) {
        String keyStrokeName = "";
        if (keyStroke != null) {
            keyStrokeName = keyStroke.toString();
        }
        ge.setString("K_" + actionName, keyStrokeName);
    }
    public static boolean keyMatches(KeyEvent e, KeyStroke ks) {
        if (e.getKeyCode() == ks.getKeyCode()) {
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == (ks.getModifiers() & InputEvent.CTRL_DOWN_MASK))
                if ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == (ks.getModifiers() & InputEvent.ALT_DOWN_MASK))
                    return (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == (ks.getModifiers() & InputEvent.SHIFT_DOWN_MASK);
        }
        return false;
    }
    public static ArrayList<Integer> pair(int x, int y)
    {
        var ar = new ArrayList<Integer>(2);
        ar.add(x);
        ar.add(y);
        return ar;
    }

    public static void addEscClose(Window window, JComponent inputMapComponent) {
        addKeyClose(window, inputMapComponent, KeyEvent.VK_ESCAPE, 0);
    }

    public static void addKeyClose(Window window, JComponent inputMapComponent, int keyCode, int modifiers) {
        var keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        String act = "key" + keyStroke.hashCode();
        inputMapComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, act);
        inputMapComponent.getActionMap().put(act, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.dispose();
            }
        });
    }



    public static void charInsert(WorldEditor worldEditor, JTextComponent editor, Component relativeTo, Object owner) {
        var ge = worldEditor.getGlobalEditor();
        ActionListener listener = e -> {
            int pos = editor.getCaretPosition();
            boolean insertNumber = false;
            boolean insertSpace = false;
            try {
                for (int checkLen = 5; checkLen <= 6; checkLen++) {
                    if (pos >= checkLen &&
                            editor.getDocument().getText(pos - checkLen, checkLen).toLowerCase().trim().equals("#char"))
                        insertNumber = true;
                        if (!editor.getDocument().getText(pos - 1, 1).equals(" ")) {
                            insertSpace = true;
                        }
                    }
                }
            catch (BadLocationException ignored) {
            }

            int c = Integer.parseInt(e.getActionCommand());
            ge.f3char = c;
            String str = "";
            if (insertNumber) {
                if (insertSpace) str = " ";
                str += Integer.toString(c);
            } else {
                if (c == 13) {
                    str = "\n";
                } else {
                    byte[] bytes = {(byte) c};
                    str = CP437.toUnicode(bytes);
                }
            }
            try {
                editor.getDocument().insertString(pos, str, null);
            } catch (BadLocationException ignored) {
            }
        };
        ColourSelector.createColourSelector(worldEditor, ge.f3char, relativeTo, owner, listener, ColourSelector.CHAR);
    }
    public static double[] rgbToLabFake(int R, int G, int B) {
        double[] lab = new double[3];

        lab[0] = R / 255.0 * 0.30;
        lab[1] = G / 255.0 * 0.59;
        lab[2] = B / 255.0 * 0.11;

        return lab;
    }
    public static double[] rgbToLab(int R, int G, int B) {
        // https://stackoverflow.com/a/45263428
        double r, g, b, X, Y, Z, xr, yr, zr;

        // D65/2°
        double Xr = 95.047;
        double Yr = 100.0;
        double Zr = 108.883;

        // --------- RGB to XYZ ---------//

        r = R/255.0;
        g = G/255.0;
        b = B/255.0;

        if (r > 0.04045)
            r = Math.pow((r+0.055)/1.055,2.4);
        else
            r = r/12.92;

        if (g > 0.04045)
            g = Math.pow((g+0.055)/1.055,2.4);
        else
            g = g/12.92;

        if (b > 0.04045)
            b = Math.pow((b+0.055)/1.055,2.4);
        else
            b = b/12.92 ;

        r*=100;
        g*=100;
        b*=100;

        X =  0.4124*r + 0.3576*g + 0.1805*b;
        Y =  0.2126*r + 0.7152*g + 0.0722*b;
        Z =  0.0193*r + 0.1192*g + 0.9505*b;


        // --------- XYZ to Lab --------- //

        xr = X/Xr;
        yr = Y/Yr;
        zr = Z/Zr;

        if ( xr > 0.008856 )
            xr =  (float) Math.pow(xr, 1/3.);
        else
            xr = (float) ((7.787 * xr) + 16 / 116.0);

        if ( yr > 0.008856 )
            yr =  (float) Math.pow(yr, 1/3.);
        else
            yr = (float) ((7.787 * yr) + 16 / 116.0);

        if ( zr > 0.008856 )
            zr =  (float) Math.pow(zr, 1/3.);
        else
            zr = (float) ((7.787 * zr) + 16 / 116.0);


        double[] lab = new double[3];

        lab[0] = (116*yr)-16;
        lab[1] = 500*(xr-yr);
        lab[2] = 200*(yr-zr);

        return lab;

    }

    public static String evalConfigDir(String string) {
        while (string.contains("<<") && string.contains(">>")) {
            int paramStart = string.indexOf("<<");
            int paramEnd = string.indexOf(">>");
            if (paramStart == -1 || paramEnd == -1 || paramEnd < paramStart) break;
            String param = string.substring(paramStart + 2, paramEnd);
            String before = string.substring(0, paramStart);
            String after = string.substring(paramEnd + 2);
            String replaceWith = System.getProperty(param);
            if (replaceWith != null) {
                string = before + replaceWith + after;
            }
        }
        return string;
    }
}
