package zedit2;

import javax.swing.text.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class CodeDocument extends DefaultStyledDocument {
    private boolean highlightEnabled;
    private Random rng = new Random();

    private SimpleAttributeSet basic;
    private SimpleAttributeSet hlObjectName;
    private SimpleAttributeSet hlNoop;
    private SimpleAttributeSet hlDollar;
    private SimpleAttributeSet hlExclamation;
    private SimpleAttributeSet hlText;
    private SimpleAttributeSet hlCenteredText;
    private SimpleAttributeSet hlTextWarn;
    private SimpleAttributeSet hlTextWarnLight;
    private SimpleAttributeSet hlError;
    private SimpleAttributeSet hlGotoLabel;
    private SimpleAttributeSet hlColon;
    private SimpleAttributeSet hlLabel;
    private SimpleAttributeSet hlZapped;
    private SimpleAttributeSet hlZappedLabel;
    private SimpleAttributeSet hlSlash;
    private SimpleAttributeSet hlDirection;
    private SimpleAttributeSet hlHash;
    private SimpleAttributeSet hlCommand;
    private SimpleAttributeSet hlThing;
    private SimpleAttributeSet hlNumber;
    private SimpleAttributeSet hlCounter;
    private SimpleAttributeSet hlFlag;
    private SimpleAttributeSet hlCondition;
    private SimpleAttributeSet hlMusicOctave, hlMusicNote, hlMusicTiming,
            hlMusicTimingMod, hlMusicDrum, hlMusicRest, hlMusicSharpFlat;
    private SimpleAttributeSet hlObjectLabelSeparator;
    private SimpleAttributeSet hlBlue, hlGreen, hlCyan, hlRed, hlPurple, hlYellow, hlWhite;

    private String[] warnings;

    private static final String TEXT_WARNING_1 = "This text will fit in a message bar but not a text box";
    private static final String TEXT_WARNING_2 = "This text is too long to show on a message bar";
    private static final String TEXT_WARNING_1_SZZT = "This text will fit in a text box but not a message bar ";
    private static final String TEXT_WARNING_2_SZZT = "This text is too long to show in a text box";

    private static final String NAME_NOT_FIRST = "@Name has no effect except at the very start of a program";
    private static final String IGNORED_EXTRA = "This has no effect";
    private static final String LABEL_IS_FIRST = "Labels do not function properly at the very start of a program";
    private static final String LABEL_CONTAINS_NUMBER = "Labels with numbers in them function strangely. Only use them if you know what you're doing";
    private static final String INVALID_DIR = "Invalid direction";
    private static final String[] PURE_DIRS = {
            "N", "S", "E", "W", "I", "NORTH", "SOUTH", "EAST", "WEST", "IDLE",
            "SEEK", "FLOW", "RND", "RNDNE", "RNDNS", "RND"};
    private static final String[] MOD_DIRS = {
            "OPP", "CW", "CCW", "RNDP"};
    private static final String[] COMMANDS = {
            "BECOME", "BIND", "CHANGE", "CHAR", "CLEAR", "CYCLE", "DIE", "END", "ENDGAME", "GIVE", "GO",
            "IDLE", "IF", "LOCK", "PLAY", "PUT", "RESTORE", "SEND", "SET", "SHOOT", "TAKE", "THEN",
            "THROWSTAR", "TRY", "UNLOCK", "WALK", "ZAP"};
    private HashMap<String, Boolean> directions = new HashMap<>();
    private HashSet<String> commands = new HashSet<>();
    private WorldData worldData;
    private WorldEditor editor;
    private GlobalEditor globalEditor;
    private boolean szzt;

    private SimpleAttributeSet colAttrib(String propertyName) {
        var attrib = new SimpleAttributeSet();
        var col = new Color(Integer.parseInt(globalEditor.getString(propertyName, "FFFFFF"), 16));

        StyleConstants.setForeground(attrib, col);
        return attrib;
    }

    public CodeDocument(WorldEditor editor) {
        this.editor = editor;
        worldData = this.editor.getWorldData();
        globalEditor = this.editor.getGlobalEditor();
        szzt = worldData.isSuperZZT();
        basic = new SimpleAttributeSet();
        var font = CP437.getFont();
        StyleConstants.setForeground(basic, Color.WHITE);
        StyleConstants.setFontSize(basic, font.getSize());
        StyleConstants.setFontFamily(basic, font.getFamily());

        highlightEnabled = globalEditor.getBoolean("SYNTAX_HIGHLIGHTING", true);

        hlCenteredText = colAttrib("HL_CENTEREDTEXT");
        hlColon = colAttrib("HL_COLON");
        hlCommand = colAttrib("HL_COMMAND");
        hlCondition = colAttrib("HL_CONDITION");
        hlCounter = colAttrib("HL_COUNTER");
        hlDirection = colAttrib("HL_DIRECTION");
        hlDollar = colAttrib("HL_DOLLAR");
        hlError = colAttrib("HL_ERROR");
        hlExclamation = colAttrib("HL_EXCLAMATION");
        hlFlag = colAttrib("HL_FLAG");
        hlGotoLabel = colAttrib("HL_GOTOLABEL");
        hlHash = colAttrib("HL_HASH");
        hlLabel = colAttrib("HL_LABEL");
        hlMusicDrum = colAttrib("HL_MUSICDRUM");
        hlMusicNote = colAttrib("HL_MUSICNOTE");
        hlMusicOctave = colAttrib("HL_MUSICOCTAVE");
        hlMusicRest = colAttrib("HL_MUSICREST");
        hlMusicSharpFlat = colAttrib("HL_MUSICSHARPFLAT");
        hlMusicTiming = colAttrib("HL_MUSICTIMING");
        hlMusicTimingMod = colAttrib("HL_MUSICTIMINGMOD");
        hlNoop = colAttrib("HL_NOOP");
        hlNumber = colAttrib("HL_NUMBER");
        hlObjectLabelSeparator = colAttrib("HL_OBJECTLABELSEPARATOR");
        hlObjectName = colAttrib("HL_OBJECTNAME");
        hlSlash = colAttrib("HL_SLASH");
        hlText = colAttrib("HL_TEXT");
        hlTextWarn = colAttrib("HL_TEXTWARN");
        hlTextWarnLight = colAttrib("HL_TEXTWARNLIGHT");
        hlThing = colAttrib("HL_THING");
        hlZapped = colAttrib("HL_ZAPPED");
        hlZappedLabel = colAttrib("HL_ZAPPEDLABEL");
        hlBlue = colAttrib("HL_BLUE");
        hlGreen = colAttrib("HL_GREEN");
        hlCyan = colAttrib("HL_CYAN");
        hlRed = colAttrib("HL_RED");
        hlPurple = colAttrib("HL_PURPLE");
        hlYellow = colAttrib("HL_YELLOW");
        hlWhite = colAttrib("HL_WHITE");

        /*
        StyleConstants.setUnderline(hlNoop, true);
        StyleConstants.setUnderline(hlTextWarn, true);
        StyleConstants.setUnderline(hlError, true);
         */
        StyleConstants.setItalic(hlNoop, true);
        StyleConstants.setItalic(hlTextWarn, true);
        StyleConstants.setItalic(hlError, true);

        for (var dir : PURE_DIRS) directions.put(dir, true);
        for (var dir : MOD_DIRS) directions.put(dir, false);
        commands.addAll(Arrays.asList(COMMANDS));
    }

    public void reHighlight(boolean insert, int len, int offset) {
        int newLen = warnings.length + (insert ? len : -len);
        if (newLen != getLength()) {
            // This can easily happen if multiple things change at once
            // Discard and rehighlight everything
            reHighlight();
            return;
        }

        String[] warningsCopy = new String[newLen];;
        int offsetStart, offsetEnd;
        if (insert) {
            System.arraycopy(warnings, 0, warningsCopy, 0, offset);
            System.arraycopy(warnings, offset, warningsCopy, offset + len, warnings.length - offset);
            offsetStart = offset;
            offsetEnd = offset + len;
        } else {
            System.arraycopy(warnings, 0, warningsCopy, 0, offset);
            System.arraycopy(warnings, offset + len, warningsCopy, offset, warningsCopy.length - offset);
            offsetStart = offset;
            offsetEnd = offset + 1;
        }
        warnings = warningsCopy;
        continueHighlight(offsetStart, offsetEnd);
    }

    public void reHighlight() {
        int len = getLength();
        warnings = new String[len];
        continueHighlight(0, len - 1);
    }
    public void continueHighlight(int offsetStart, int offsetEnd) {
        int len = getLength();
        String text;
        try {
            text = getText(0, len);
        } catch (BadLocationException e) {
            return;
        }
        int start = findLineBeginning(text, offsetStart);
        int end = findLineEnding(text, offsetEnd);
        setCharacterAttributes(start, end - start, basic, true);
        for (int pos = start; pos < end; pos++) {
            if (text.charAt(pos) == '\n') {
                highlight(text, start, pos, false);
                start = pos + 1;
            }
        }
        highlight(text, start, end, false);
    }

    int findLineBeginning(String text, int pos) {
        for (;;) {
            if (pos <= 0) return 0;
            if (text.charAt(pos - 1) == '\n') return pos;
            pos--;
        }
    }

    int findLineEnding(String text, int pos) {
        if (pos < 0) pos = 0;
        for (;;) {
            if (pos >= text.length()) return text.length();
            if (text.charAt(pos) == '\n') return pos;
            pos++;
        }
    }

    private void highlight(String text, int start, int end, boolean assumeCommand)
    {
        if (start == -1) return;
        if (!highlightEnabled) return;
        int prevStart = start;
        for (;;) {
            if (start == end) return;
            if (text.charAt(start) == ' ') start++;
            else break;
        }
        char firstChar = Character.toUpperCase(text.charAt(start));
        if (firstChar == '@') {
            if (start == 0) {
                setCharacterAttributes(start, end - start, hlObjectName, false);
            } else {
                setCharacterAttributes(start, end - start, hlNoop, false);
                setWarning(start, end - start, NAME_NOT_FIRST);
            }
            return;
        }
        if (firstChar == '\'') {
            highlightLabel(text, start, end, true);
            return;
        }
        if (firstChar == ':') {
            highlightLabel(text, start, end, false);
            return;
        }
        if (firstChar == '#') {
            highlightCommand(text, start, end, !assumeCommand);
            return;
        }
        if (firstChar == '/' || firstChar == '?') {
            handleDirection(text, start, start, end, true);
            return;
        }
        if (assumeCommand) {
            var p = scanIn(text, start, end);
            if (p.isEmpty()) {
                highlightText(text, start, end);
                return;
            } else {
                highlightCommand(text, start, end, false);
                return;
            }
        }
        if (!assumeCommand) start = prevStart;
        highlightText(text, start, end);
    }

    private void highlightText(String text, int start, int end) {
        // Max length of a message bar line: 50 characters
        // Max sensible length of object name: 45 characters
        // Max length of a textbox line: 43 characters
        // Max length of a $ textbox line: 45 characters
        // Max length of a ! textbox line: 38

        char firstChar = text.charAt(start);
        if (firstChar == '$') {
            setCharacterAttributes(start, 1, hlDollar, false);
            highlightTextLine(start + 1, end, 42, 49, hlCenteredText, hlTextWarnLight, hlTextWarn);
            return;
        }
        if (firstChar == '!') {
            int semicolon_pos = text.indexOf(';', start + 1);
            if (semicolon_pos == -1 || semicolon_pos >= end) {
                setCharacterAttributes(start, 1, hlExclamation, false);
                highlightTextLine(start + 1, end, 38, 49, hlText, hlTextWarnLight, hlTextWarn);
            } else {
                semicolon_pos -= start + 1;
                var el = getCharacterElement(start);
                setCharacterAttributes(start, 1, hlExclamation, false);
                setCharacterAttributes(start + 1, semicolon_pos, hlGotoLabel, false);
                setCharacterAttributes(start + 1 + semicolon_pos, 1, hlExclamation, false);
                highlightTextLine(start + 1 + semicolon_pos + 1, end, 38, 48 - semicolon_pos, hlText, hlTextWarnLight, hlTextWarn);
            }
            return;
        }
        highlightTextLine(start, end, 42, 50, hlText, hlTextWarnLight, hlTextWarn);
    }

    private void highlightTextLine(int start, int end, int warn1, int warn2, SimpleAttributeSet hlNormal, SimpleAttributeSet hlWarn1, SimpleAttributeSet hlWarn2) {
        if (szzt) {
            // Super ZZT actually has the message box length longer than the message bar length
            // So modify then swap these
            warn1 -= (42 - 29);
            warn2 -= (50 - 26);

            var t = warn1;
            warn1 = warn2;
            warn2 = t;
        }

        int len = end - start;
        int lenWarn1 = Math.max(0, len - warn1);
        int lenWarn2 = Math.max(0, len - warn2);
        len -= lenWarn1;
        lenWarn1 -= lenWarn2;

        setCharacterAttributes(start, len, hlNormal, false);
        setCharacterAttributes(start + len, lenWarn1, hlWarn1, false);
        setCharacterAttributes(start + len + lenWarn1, lenWarn2, hlWarn2, false);

        if (!szzt) {
            setWarning(start + len, lenWarn1, TEXT_WARNING_1);
            setWarning(start + len + lenWarn1, lenWarn2, TEXT_WARNING_2);
        } else {
            setWarning(start + len, lenWarn1, TEXT_WARNING_1_SZZT);
            setWarning(start + len + lenWarn1, lenWarn2, TEXT_WARNING_2_SZZT);
        }

    }

    private void setWarning(int start, int len, String warning) {
        for (int i = 0; i < len; i++) {
            warnings[i + start] = warning;
        }
    }

    private void highlightLabel(String text, int start, int end, boolean zapped) {
        if (start == 0 && !zapped) {
            setCharacterAttributes(start, end, hlTextWarn, false);
            setWarning(start, end, LABEL_IS_FIRST);
        } else {
            boolean warn = false;
            if (!zapped) {
                for (int i = start; i < end; i++) {
                    char c = text.charAt(i);
                    if (c >= '0' && c <= '9') {
                        warn = true;
                        break;
                    }
                }
            }
            setCharacterAttributes(start, 1, zapped ? hlZapped : hlColon, false);
            setCharacterAttributes(start + 1, end - start - 1, zapped ? hlZappedLabel : hlLabel, false);
            if (warn) {
                setCharacterAttributes(start, end - start, hlTextWarn, false);
                setWarning(start, end - start, LABEL_CONTAINS_NUMBER);
            }
        }
    }

    private int handleDirection(String text, int lineStart, int start, int end, boolean moveCharacter) {
        int pos = start;
        int startOffset = 0;
        if (moveCharacter) {
            setCharacterAttributes(start, 1, hlSlash, false);
            startOffset = 1;
            pos++;
        }
        // Scan in movement command. Bring in a-z 0-9 : and _
        pos = skipSpaces(text, pos, end);
        int prevPos = pos;
        var mov = scanIn(text, pos, end);
        var isPureDir = directions.get(mov);
        pos += mov.length();
        if (isPureDir == null) {
            // Not a direction
            setCharacterAttributes(lineStart, pos - lineStart, hlError, false);
            setWarning(lineStart, pos - lineStart, INVALID_DIR);
        } else {
            setCharacterAttributes(prevPos, pos - prevPos, hlDirection, false);

            while (!isPureDir) {
                pos = skipSpaces(text, pos, end);
                mov = scanIn(text, pos, end);
                int endPos = pos + mov.length();
                isPureDir = directions.get(mov);
                if (isPureDir == null) {
                    setCharacterAttributes(start, endPos - start, hlError, false);
                    setWarning(start, endPos - start, INVALID_DIR);
                    pos = endPos;
                    break;
                } else {
                    setCharacterAttributes(pos, endPos - pos, hlDirection, false);
                    pos = endPos;
                }
            }
        }

        pos = skipSpaces(text, pos, end);

        if (moveCharacter) {
            // Single-character movement optionally takes a number in Super ZZT.
            if (szzt) {
                var nextp = scanIn(text, pos, end);
                var val = parseNumber(nextp, 0, 255);
                if (val != null) {
                    setCharacterAttributes(pos, nextp.length(), hlNumber, false);
                    pos += nextp.length();
                    pos = skipSpaces(text, pos, end);
                }
                if (text.charAt(start) == '?') {
                    setCharacterAttributes(start, pos - start, hlTextWarn, false);
                    setWarning(start, pos - start, "Warning: ? has unusual behaviour in Super ZZT");
                }
            }

            highlight(text, pos, end, false);
            return -1;
        }
        return pos;
    }

    private String scanIn(String text, int start, int end) {
        var scan = new StringBuilder();
        boolean consumeSpaces = true;
        for (;;) {
            if (start >= end) return scan.toString();
            char c = Character.toUpperCase(text.charAt(start));
            if (c == ' ' && consumeSpaces) {
                start++;
                continue;
            }

            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == ':') || (c == '_')) {
                consumeSpaces = false;
                scan.append(c);
            } else {
                return scan.toString();
            }
            start++;
        }
    }

    private SimpleAttributeSet parseColour(String text) {
        switch (text) {
            case "BLUE": return hlBlue;
            case "GREEN": return hlGreen;
            case "CYAN": return hlCyan;
            case "RED": return hlRed;
            case "PURPLE": return hlPurple;
            case "YELLOW": return hlYellow;
            case "WHITE": return hlWhite;
            default: return null;
        }
    }
    private boolean isType(String text) {
        switch (text) {
            case "AMMO": case "BEAR": case "BLINKWALL": case "BOMB": case "BOULDER": case "BULLET": case "BREAKABLE":
            case "CLOCKWISE": case "COUNTER": case "DOOR": case "DUPLICATOR": case "EMPTY": case "ENERGIZER":
            case "FAKE": case "FOREST": case "GEM": case "HEAD": case "INVISIBLE": case "KEY": case "LINE":
            case "LION": case "MONITOR": case "NORMAL": case "OBJECT": case "PASSAGE": case "PLAYER": case "PUSHER":
            case "RICOCHET": case "RUFFIAN": case "SCROLL": case "SEGMENT": case "SLIDEREW": case "SLIDERNS":
            case "SLIME": case "SOLID": case "SPINNINGGUN": case "STAR": case "TIGER": case "TRANSPORTER":
                return true;
            case "SHARK": case "TORCH": case "WATER":
                return !szzt;
            case "LAVA": case "FLOOR": case "WATERN": case "WATERS": case "WATERE": case "WATERW":
            case "ROTON": case "DRAGONPUP": case "PAIRER": case "SPIDER": case "WEB": case "STONE":
                return szzt;
            default:
                return false;
        }
    }

    private void highlightCommand(String text, int start, int end, boolean leadingHash) {
        int pos = start;
        if (leadingHash) {
            setCharacterAttributes(start, 1, hlHash, false);
            pos++;
            while (pos < end) {
                if (text.charAt(pos) == ' ') {
                    pos++;
                } else {
                    break;
                }
            }
        }
        while (pos < end) {
            if (text.charAt(pos) == '#') {
                setCharacterAttributes(pos, 1, hlNoop, false);
                setWarning(pos, 1, "Unnecessary #");
                pos++;
            } else {
                break;
            }
        }
        var cmd = scanIn(text, pos, end);

        if (commands.contains(cmd)) {
            // This is a valid command. Let's see what params it takes
            setCharacterAttributes(pos, cmd.length(), hlCommand, false);
            int beforePos = pos;
            pos += cmd.length();

            switch (cmd) {
                case "BECOME": {
                    pos = handleColourThing(text, start, pos, end, false);
                    pos = ignoreExtra(text, pos, end);
                    break; }
                case "BIND": {
                    pos = skipSpaces(text, pos, end);

                    var p = scanIn(text, pos, end);
                    if (p.isEmpty()) {
                        setCharacterAttributes(start, end - start, hlNoop, false);
                        setWarning(start, end - start, "Missing bind target");
                    } else {
                        setCharacterAttributes(pos, p.length(), hlObjectName, false);
                    }
                    pos += p.length();
                    pos = ignoreExtra(text, pos, end);
                    break; }
                case "CHANGE": {
                    pos = handleColourThing(text, start, pos, end, true);
                    pos = handleColourThing(text, start, pos, end, true);
                    pos = ignoreExtra(text, pos, end);
                    break; }
                case "CHAR": {
                    pos = skipSpaces(text, pos, end);
                    var p = scanIn(text, pos, end);
                    Integer charVal = parseNumber(p, 1, 255);

                    if (charVal == null) {
                        setCharacterAttributes(start, end - start, hlNoop, false);
                        setWarning(start, end - start, "Missing/invalid char value");
                    } else {
                        setCharacterAttributes(pos, p.length(), hlNumber, false);
                        var charUni = CP437.toUnicode(new byte[]{charVal.byteValue()}).charAt(0);
                        String tooltip = String.format("<html><font size=\"5\" face=\"%s\">&#%d;</body></html>", CP437.getFont().getFamily(), (int) charUni);
                        setWarning(pos, p.length(), tooltip);
                    }
                    pos += p.length();
                    pos = ignoreExtra(text, pos, end);
                    break; }
                case "SET":
                case "CLEAR": {
                    pos = handleFlagName(text, start, pos, end);
                    break;
                }
                case "CYCLE": {
                    pos = skipSpaces(text, pos, end);
                    var p = scanIn(text, pos, end);
                    Integer cycleVal = parseNumber(p, 1, 255);
                    if (cycleVal == null) {
                        setCharacterAttributes(start, end - start, hlNoop, false);
                        setWarning(start, end - start, "Missing/invalid cycle value");
                    } else {
                        setCharacterAttributes(pos, p.length(), hlNumber, false);
                    }
                    pos += p.length();
                    pos = ignoreExtra(text, pos, end);
                    break;
                }
                case "DIE":
                case "END":
                case "ENDGAME":
                case "IDLE":
                case "LOCK":
                case "UNLOCK":
                    pos = ignoreExtra(text, pos, end);
                    break;
                case "TAKE":
                case "GIVE": {
                    pos = handleCounter(text, start, pos, end);
                    pos = handleNumber(text, start, pos, end, 0, 32767, true);
                    highlight(text, pos, end, true);
                    break;
                }
                case "WALK":
                case "GO": {
                    pos = handleDirection(text, start, pos, end, false);
                    pos = ignoreExtra(text, pos, end);
                    break;
                }
                case "IF": {
                    pos = handleCondition(text, start, pos, end);
                    highlight(text, pos, end, true);
                    break;
                }
                case "PLAY": {
                    var playMus = text.substring(pos, end);
                    var playDur = Audio.getPlayDuration(playMus);
                    String cycles;
                    if (playDur % 2 == 1) {
                        cycles = String.format("%.1f", playDur * 0.5);
                    } else {
                        cycles = String.valueOf(playDur / 2);
                    }
                    setWarning(start, end - start, String.format("Play duration length: %s cycle%s at default speed", cycles, playDur == 2 ? "" : "s"));
                    for (int i = pos; i < end; i++) {
                        char c = Character.toUpperCase(text.charAt(i));
                        switch (c) {
                            case 'T': case 'S': case 'I': case 'Q': case 'H': case 'W':
                                setCharacterAttributes(i, 1, hlMusicTiming, false);
                                break;
                            case '0': case '1': case '2': case '4': case '5':
                            case '6': case '7': case '8': case '9':
                                setCharacterAttributes(i, 1, hlMusicDrum, false);
                                break;
                            case '+': case '-':
                                setCharacterAttributes(i, 1, hlMusicOctave, false);
                                break;
                            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G':
                                setCharacterAttributes(i, 1, hlMusicNote, false);
                                break;
                            case 'X':
                                setCharacterAttributes(i, 1, hlMusicRest, false);
                                break;
                            case '#': case '!':
                                setCharacterAttributes(i, 1, hlMusicSharpFlat, false);
                                break;
                            case '3': case '.':
                                setCharacterAttributes(i, 1, hlMusicTimingMod, false);
                                break;
                            case ' ': case '[': case ']':
                                break;
                            default:
                                setCharacterAttributes(i, 1, hlError, false);
                                setWarning(start, end - start, "Invalid command in #PLAY sequence");
                                break;
                        }
                    }
                    break;
                }
                case "PUT": {
                    pos = handleDirection(text, start, pos, end, false);
                    pos = handleColourThing(text, start, pos, end, false);
                    pos = ignoreExtra(text, pos, end);
                    break;
                }
                case "ZAP":
                case "RESTORE": {
                    pos = handleLabel(text, start, pos, end, false);
                    pos = ignoreExtra(text, pos, end);
                    break;
                }
                case "SEND": {
                    pos = handleLabel(text, start, pos, end, false);
                    pos = ignoreExtra(text, pos, end);
                    break;
                }
                case "THROWSTAR":
                case "SHOOT": {
                    pos = handleDirection(text, start, pos, end, false);
                    pos = ignoreExtra(text, pos, end);
                    break;
                }
                case "THEN": {
                    setCharacterAttributes(beforePos, pos - beforePos, hlNoop, false);
                    setWarning(beforePos, pos - beforePos, "This command does nothing and can be removed");
                    pos = skipSpaces(text, pos, end);
                    highlight(text, pos, end, true);
                    break;
                }
                case "TRY": {
                    pos = handleDirection(text, start, pos, end, false);
                    highlight(text, pos, end, true);
                    break;
                }
                default:
                    break;
            }
        } else {
            // This is an abbreviated SEND instead
            pos = handleLabel(text, start, pos, end, false);
            pos = ignoreExtra(text, pos, end);
            /*
            setCharacterAttributes(pos, cmd.length(), hlGotoLabel, false);
            pos += cmd.length();
            setCharacterAttributes(pos, end - pos, hlNoop, false);
            setWarning(pos, end - pos, IGNORED_EXTRA);
            */
        }
    }

    private int handleLabel(String text, int start, int pos, int end, boolean optional) {
        if (pos == -1) return pos;
        pos = skipSpaces(text, pos, end);
        var p = scanIn(text, pos, end);
        if (p.isEmpty()) {
            if (optional) return pos;

            setWarning(start, end - start, "Missing/invalid label name");
            setCharacterAttributes(start, end - start, hlError, false);
            return -1;
        }
        setCharacterAttributes(pos, p.length(), hlGotoLabel, false);
        int colon = p.indexOf(':');
        if (colon != -1) {
            setCharacterAttributes(pos, colon, hlObjectName, false);
            setCharacterAttributes(pos + colon, 1, hlObjectLabelSeparator, false);
        }
        pos += p.length();
        return pos;
    }

    private int handleCondition(String text, int start, int pos, int end) {
        if (pos == -1) return pos;
        pos = skipSpaces(text, pos, end);
        int beforePos = pos;
        var p = scanIn(text, pos, end);
        setCharacterAttributes(pos, p.length(), hlCondition, false);
        switch (p) {
            case "ALLIGNED":
            case "CONTACT":
            case "ENERGIZED":
                pos += p.length();
                break;
            case "ALIGNED":
                setWarning(pos, p.length(), "Did you mean ALLIGNED?");
                setCharacterAttributes(pos, p.length(), hlTextWarn, false);
                pos += p.length();
                break;
            case "ANY":
                pos += p.length();
                pos = handleColourThing(text, start, pos, end, true);
                break;
            case "BLOCKED":
                pos += p.length();
                pos = handleDirection(text, pos - p.length(), pos, end, false);
                break;
            case "NOT":
                pos += p.length();
                pos = handleCondition(text, start, pos, end);
                break;
            case "":
                setWarning(start, end - start, "Missing conditional expression");
                setCharacterAttributes(start, end - start, hlError, false);
                return -1;
            default:
                pos = handleFlagName(text, start, pos, end);
                break;
        }
        return pos;
    }

    private int handleCounter(String text, int start, int pos, int end) {
        if (pos == -1) return pos;
        pos = skipSpaces(text, pos, end);
        var p = scanIn(text, pos, end);
        boolean valid = false;
        switch (p) {
            case "AMMO": case "GEMS": case "HEALTH": case "SCORE": case "TIME":
                valid = true;
                break;
            case "TORCHES":
                valid = !szzt;
                break;
            case "Z":
                valid = szzt;
                break;
        }
        if (!valid) {
            setWarning(start, end - start, "Missing/invalid counter name");
            setCharacterAttributes(start, end - start, hlError, false);
            return -1;
        }
        setCharacterAttributes(pos, p.length(), hlCounter, false);
        pos += p.length();
        return pos;
    }

    private int handleNumber(String text, int start, int pos, int end, int min, int max, boolean error) {
        if (pos == -1) return pos;
        pos = skipSpaces(text, pos, end);
        var p = scanIn(text, pos, end);
        Integer num = parseNumber(p, min, max);
        if (num == null) {
            setWarning(start, end - start, "Missing/invalid value");
            if (error) {
                setCharacterAttributes(start, end - start, hlError, false);
            } else {
                setCharacterAttributes(start, end - start, hlNoop, false);
            }
            return -1;
        } else {
            setCharacterAttributes(pos, p.length(), hlNumber, false);
        }
        pos += p.length();

        return pos;
    }

    private Integer parseNumber(String text, int min, int max) {
        if (!text.isEmpty()) {
            if (text.matches(".*\\d.*")) {
                try {
                    int val = Integer.parseInt(text);
                    if (val >= min && val <= max) {
                        return val;
                    }
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private int handleFlagName(String text, int start, int pos, int end) {
        if (pos == -1) return pos;
        pos = skipSpaces(text, pos, end);
        var p = scanIn(text, pos, end);
        if (p.isEmpty()) {
            setCharacterAttributes(start, end - start, hlError, false);
            setWarning(start, end - start, "Invalid flag name");
            return -1;
        }
        setCharacterAttributes(pos, p.length(), hlFlag, false);
        pos += p.length();
        return pos;
    }

    private int ignoreExtra(String text, int pos, int end) {
        if (pos == -1) return pos;
        pos = skipSpaces(text, pos, end);
        setCharacterAttributes(pos, end - pos, hlNoop, false);
        setWarning(pos, end - pos, IGNORED_EXTRA);
        return pos;
    }

    private int handleColourThing(String text, int start, int pos, int end, boolean missingError) {
        if (pos == -1) return pos;
        pos = skipSpaces(text, pos, end);
        var p1 = scanIn(text, pos, end);
        boolean finished = false;
        boolean error = false;
        AttributeSet col = null;
        if (!p1.isEmpty()) {
            col = parseColour(p1);
            if (col != null) {
                setCharacterAttributes(pos, p1.length(), col, false);
                pos += p1.length();
            }
            pos = skipSpaces(text, pos, end);
            var p2 = scanIn(text, pos, end);
            if (!p2.isEmpty()) {
                if (isType(p2)) {
                    setCharacterAttributes(pos, p2.length(), hlThing, false);
                    finished = true;
                }
                pos += p2.length();
            }
        }
        //if (!finished && (missingError || col == null)) {
        if (!finished && missingError) {
            error = true;
        }
        if (error) {
            setCharacterAttributes(start, end - start, hlError, false);
            setWarning(start, end - start, "Invalid colour/type");
            return -1;
        }
        return pos;
    }

    private int skipSpaces(String text, int start, int end)
    {
        for (;;) {
            if (start == end) return start;
            if (text.charAt(start) != ' ') return start;
            start++;
        }
    }

    public String getWarning(int pos) {
        if (pos < 0 || pos >= warnings.length) return null;
        return warnings[pos];
    }
}
