package zedit2;

public class SZZTType extends ZType {
    private static final int[] charcodes = {32, 32, 32, 2, 2, 132, 32, 4, 12, 10, 232, 240, // 0 to 11
            250, 11, 127, 32, 47, 92, 32, 111, 176, 219, 178, 177, 254, 18, 29, 178, 32, // 12 to 28
            206, 0, 249, 42, 32, 235, 5, 32, 42, 32, 24, 31, 234, 227, 32, 233, 79, //
            32, 0xB0, 0x1E, 0x1F, 0x11, 0x10, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
            0x94, 0xED, 0xE5, 0x0F, 0xC5, 0x5A, 0x20, 0x20, 0x20, 0x20, 0xF8, 0xCD, 0xBA, 179};
    private static final int[] textcols = {31, 47, 63, 79, 95, 111, 15, 143, 159, 175, 191, 207, 223, 239, 255, 0, 0, 0, 0, 0, 0, 0, 127};
    private static final String[] kindnames = {"Empty", "BoardEdge", "Messenger", "Monitor", "Player", "Ammo", null,
            "Gem", "Key", "Door", "Scroll", "Passage", "Duplicator", "Bomb", "Energizer", null, "Clockwise",
            "Counter", null, "Lava", "Forest", "Solid", "Normal", "Breakable", "Boulder", "SliderNS", "SliderEW",
            "Fake", "Invisible", "BlinkWall", "Transporter", "Line", "Ricochet", null, "Bear", "Ruffian",
            "Object", "Slime", null, "SpinningGun", "Pusher", "Lion", "Tiger", null, "Head", "Segment",
            null, "Floor", "WaterN", "WaterS", "WaterW", "WaterE", null, null, null, null, null, null, null, "Roton",
            "DragonPup", "Pairer", "Spider", "Web", "Stone", null, null, null, null, "Bullet", "HBlinkRay", "VBlinkRay", "Star",
            "BlueText", "GreenText", "CyanText", "RedText", "PurpleText", "BrownText", "BlackText",
            "BlackBText", "BlueBText", "GreenBText", "CyanBText", "RedBText", "PurpleBText", "BrownBText", "GreyBText",
            null, null, null, null, null, null, null, "GreyText"};
    private static final int[] linechars = {206, 204, 185, 186, 202, 200, 188, 208, 203, 201, 187, 210, 205, 198, 181, 249};
    private static final int[] webchars =  {197, 195, 180, 179, 193, 192, 217, 179, 194, 218, 191, 179, 196, 196, 196, 250};

    private static final int[] duplicatorFrames = {250, 250, 249, 248, 111, 79};

    private static boolean isLineOrEdge(int id) {
        return (id == LINE) || (id == BOARDEDGE);
    }
    private static boolean isWebOrEdge(Tile tile) {
        if ((tile.getId() == WEB) || (tile.getId() == BOARDEDGE)) {
            return true;
        }
        var stats = tile.getStats();
        for (var stat : stats) {
            if (stat.getUid() == WEB) return true;
        }
        return false;
    }
    private static Stat getLastStat(Board board, int x, int y)
    {
        var stats = board.getStatsAt(x, y);
        if (stats.isEmpty()) return null;
        return stats.get(stats.size() - 1);
    }
    public static String getName(int id)
    {
        if (id >= kindnames.length) return null;
        return kindnames[id];
    }
    public static int getChar(Board board, int x, int y)
    {
        int id = board.getTileId(x, y);

        switch (id) {
            case DUPLICATOR: {
                var lastStat = getLastStat(board, x, y);
                if (lastStat == null) break;
                return duplicatorFrames[lastStat.getP1()];
            }
            case BOMB: {
                var lastStat = getLastStat(board, x, y);
                if (lastStat == null) break;
                int bombChar = (48 + lastStat.getP1()) & 0xFF;
                if (bombChar == 48 || bombChar == 49) bombChar = 11;
                return bombChar;
            }
            case TRANSPORTER: {
                var lastStat = getLastStat(board, x, y);
                if (lastStat == null) break;
                int xs = lastStat.getStepX();
                int ys = lastStat.getStepY();
                if (xs < 0 && ys == 0) return 40; // '('
                if (xs > 0 && ys == 0) return 41; // ')'
                if (xs == 0 && ys > 0) return 118; // 'v'
                return 94; // '^'
            }
            case OBJECT: {
                var lastStat = getLastStat(board, x, y);
                if (lastStat == null) break;
                return lastStat.getP1();
            }
            case PUSHER: {
                var lastStat = getLastStat(board, x, y);
                if (lastStat == null) break;
                int xs = lastStat.getStepX();
                int ys = lastStat.getStepY();
                if (xs < 0 && ys == 0) return 17;
                if (xs > 0 && ys == 0) return 16;
                if (xs == 0 && ys < 0) return 30;
                return 31;
            }
            case LINE: {
                // For lines, the char depends on surrounding tiles
                int lch = 15;
                if ((y == 0) || isLineOrEdge(board.getTileId(x, y - 1))) lch -= 8;
                if ((y == board.getHeight() - 1) || isLineOrEdge(board.getTileId(x, y + 1))) lch -= 4;
                if ((x == board.getWidth() - 1) || isLineOrEdge(board.getTileId(x + 1, y))) lch -= 2;
                if ((x == 0) || isLineOrEdge(board.getTileId(x - 1, y))) lch -= 1;
                return linechars[lch];
            }
            case WEB: {
                // For webs, the char depends on surrounding tiles
                int lch = 15;
                if ((y == 0) || isWebOrEdge(board.getTile(x, y - 1, false))) lch -= 8;
                if ((y == board.getHeight() - 1) || isWebOrEdge(board.getTile(x, y + 1, false))) lch -= 4;
                if ((x == board.getWidth() - 1) || isWebOrEdge(board.getTile(x + 1, y, false))) lch -= 2;
                if ((x == 0) || isWebOrEdge(board.getTile(x - 1, y, false))) lch -= 1;
                return webchars[lch];
            }
            case BLUETEXT:
            case GREENTEXT:
            case CYANTEXT:
            case REDTEXT:
            case PURPLETEXT:
            case BROWNTEXT:
            case BLACKTEXT:
            case BLACKBTEXT:
            case BLUEBTEXT:
            case GREENBTEXT:
            case CYANBTEXT:
            case REDBTEXT:
            case PURPLEBTEXT:
            case BROWNBTEXT:
            case GREYBTEXT:
            case GREYTEXT: {
                // For text kinds, the char is the colour
                return board.getTileCol(x, y);
            }
            case INVISIBLE: {
                if (GlobalEditor.getGlobalEditor().getBoolean("SHOW_INVISIBLES", false)) {
                    return 176;
                } else {
                    return 32;
                }
            }
        }
        // Otherwise, check in the charcodes list

        if (id < charcodes.length) return charcodes[id];
        // Otherwise, return '?'
        return 63;
    }

    public static int getColour(Board board, int x, int y)
    {
        int id = board.getTileId(x, y);

        switch (id) {
            case EMPTY: {
                // Empty is a little special- it's always black
                return 0x00;
            }
            case PLAYER: {
                // With stats, the player is c1F
                if (!board.getStatsAt(x, y).isEmpty()) {
                    return 0x1F;
                }
                break;
            }
            case BLUETEXT:
            case GREENTEXT:
            case CYANTEXT:
            case REDTEXT:
            case PURPLETEXT:
            case BROWNTEXT:
            case BLACKTEXT:
            case BLACKBTEXT:
            case BLUEBTEXT:
            case GREENBTEXT:
            case CYANBTEXT:
            case REDBTEXT:
            case PURPLEBTEXT:
            case BROWNBTEXT:
            case GREYBTEXT:
            case GREYTEXT: {
                // For text kinds, the colour is based on the kind
                return getTextColour(id);
            }
            default:
                break;
        }
        // Otherwise, use the given colour
        return board.getTileCol(x, y);
    }

    public static int getTextColour(int id) {
        if (id - BLUETEXT < 0 || id - BLUETEXT >= textcols.length) return -1;
        return textcols[id - BLUETEXT];
    }
    public static boolean isCrashy(Tile tile) {
        return tile.getId() >= BLACKBTEXT;
    }
    public static int getTextId(int colour) {
        if (colour == 0x7F) return GREYTEXT;
        int id = (colour / 16) - 1 + BLUETEXT;
        if (id == (BLUETEXT - 1)) id = BLACKTEXT;
        return id;
    }

    public static boolean isFloor(Tile tile) {
        int id = tile.getId();
        if (id == EMPTY ||
            id == FAKE ||
            id == LAVA ||
            id == FLOOR ||
            id == WEB ||
            id == WATERN ||
            id == WATERS ||
            id == WATERW ||
            id == WATERE) return true;
        else return false;
    }





    public static final int LAVA = 19;

    public static final int FLOOR = 47;
    public static final int WATERN = 48;
    public static final int WATERS = 49;
    public static final int WATERW = 50;
    public static final int WATERE = 51;

    public static final int ROTON = 59;
    public static final int DRAGONPUP = 60;
    public static final int PAIRER = 61;
    public static final int SPIDER = 62;
    public static final int WEB = 63;
    public static final int STONE = 64;

    public static final int BULLET = 69;
    public static final int HBLINKRAY = 70;
    public static final int VBLINKRAY = 71;
    public static final int STAR = 72;
    public static final int BLUETEXT = 73;
    public static final int GREENTEXT = 74;
    public static final int CYANTEXT = 75;
    public static final int REDTEXT = 76;
    public static final int PURPLETEXT = 77;
    public static final int BROWNTEXT = 78;
    public static final int BLACKTEXT = 79;
    public static final int BLACKBTEXT = 80;
    public static final int BLUEBTEXT = 81;
    public static final int GREENBTEXT = 82;
    public static final int CYANBTEXT = 83;
    public static final int REDBTEXT = 84;
    public static final int PURPLEBTEXT = 85;
    public static final int BROWNBTEXT = 86;
    public static final int GREYBTEXT = 87;
    public static final int GREYTEXT = 95;
}
