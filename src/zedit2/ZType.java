package zedit2;

import java.util.HashMap;

public class ZType {
    private static HashMap<String, Integer> zztTypes = null;
    private static HashMap<String, Integer> szztTypes = null;

    public static String getName(boolean szzt, int id) {
        String name = null;
        if (id == -1) return "Unknown";
        if (!szzt) {
            name = ZZTType.getName(id);
        } else {
            name = SZZTType.getName(id);
        }
        if (name == null) {
            name = String.format("Unknown (%d)", id);
        }
        return name;
    }
    public static int getId(boolean szzt, String name) {
        HashMap<String, Integer> types;
        if (!szzt) {
            if (zztTypes == null) {
                zztTypes = buildTypesMap(szzt);
            }
            types = zztTypes;
        } else {
            if (szztTypes == null) {
                szztTypes = buildTypesMap(szzt);
            }
            types = szztTypes;
        }
        return types.getOrDefault(name, -1);
    }

    private static HashMap<String, Integer> buildTypesMap(boolean szzt) {
        HashMap<String, Integer> types = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            types.put(getName(szzt, i), i);
        }
        return types;
    }

    public static int getChar(boolean szzt, Tile tile)
    {
        if (tile.getId() == -1) return '?';
        BufferBoard board = new BufferBoard(szzt, 1, 1);
        board.setTile(0, 0, tile);
        if (!szzt) {
            return ZZTType.getChar(board, 0, 0);
        } else {
            return SZZTType.getChar(board, 0, 0);
        }
    }
    public static int getColour(boolean szzt, Tile tile)
    {
        if (tile.getId() == -1) return 15;
        BufferBoard board = new BufferBoard(szzt, 1, 1);
        board.setTile(0, 0, tile);
        if (!szzt) {
            return ZZTType.getColour(board, 0, 0);
        } else {
            return SZZTType.getColour(board, 0, 0);
        }
    }
    public static boolean isFloor(boolean szzt, Tile tile)
    {
        if (!szzt) {
            return ZZTType.isFloor(tile);
        } else {
            return SZZTType.isFloor(tile);
        }
    }
    public static int getTextColour(boolean szzt, int id) {
        if (!szzt) {
            return ZZTType.getTextColour(id);
        } else {
            return SZZTType.getTextColour(id);
        }
    }
    public static boolean isCrashy(boolean szzt, Tile tile) {
        if (!szzt) {
            return ZZTType.isCrashy(tile);
        } else {
            return SZZTType.isCrashy(tile);
        }
    }
    public static boolean isText(boolean szzt, int id) {
        return getTextColour(szzt, id) != -1;
    }
    public static int getTextId(boolean szzt, int colour) {
        if (!szzt) {
            return ZZTType.getTextId(colour);
        } else {
            return SZZTType.getTextId(colour);
        }
    }

    public static Tile convert(Tile input, boolean szzt) {
        var stats = input.getStats();
        var newPadding = new byte[szzt ? 0 : 8];
        for (var stat : stats) {
            stat.setPadding(newPadding);
        }
        var tileId = input.getId();
        if (szzt) { // ZZT to SZZT
            switch (tileId) {
                case ZZTType.TORCH:
                case ZZTType.SHARK:
                case ZZTType.WATER:
                    tileId = ZZTType.EMPTY;
                    stats = null;
                    break;
                case ZZTType.STAR: tileId = SZZTType.STAR; break;
                case ZZTType.BULLET: tileId = SZZTType.BULLET; break;
                case ZZTType.HBLINKRAY: tileId = SZZTType.HBLINKRAY; break;
                case ZZTType.VBLINKRAY: tileId = SZZTType.VBLINKRAY; break;
                default:
                    if (tileId >= ZZTType.BLUETEXT && tileId <= ZZTType.GREYTEXT)
                        tileId = tileId - ZZTType.BLUETEXT + SZZTType.BLUETEXT;
                    break;
            }
        } else {
            switch (tileId) {
                case SZZTType.LAVA:
                case SZZTType.FLOOR:
                case SZZTType.WATERN:
                case SZZTType.WATERE:
                case SZZTType.WATERW:
                case SZZTType.WATERS:
                case SZZTType.ROTON:
                case SZZTType.DRAGONPUP:
                case SZZTType.PAIRER:
                case SZZTType.SPIDER:
                case SZZTType.WEB:
                case SZZTType.STONE:
                    tileId = ZZTType.EMPTY;
                    stats = null;
                    break;
                case SZZTType.STAR: tileId = ZZTType.STAR; break;
                case SZZTType.BULLET: tileId = ZZTType.BULLET; break;
                case SZZTType.HBLINKRAY: tileId = ZZTType.HBLINKRAY; break;
                case SZZTType.VBLINKRAY: tileId = ZZTType.VBLINKRAY; break;
                default:
                    if (tileId >= SZZTType.BLUETEXT && tileId <= SZZTType.GREYTEXT)
                        tileId = tileId - SZZTType.BLUETEXT + ZZTType.BLUETEXT;
                    break;
            }
        }

        return new Tile(tileId, input.getCol(), stats);
    }

    public static Tile[] convert(Tile[] input, boolean szzt) {
        var outputTiles = new Tile[input.length];
        for (int i = 0; i < input.length; i++) {
            outputTiles[i] = convert(input[i], szzt);
        }
        return outputTiles;
    }

    public static final int EMPTY = 0;
    public static final int BOARDEDGE = 1;
    public static final int MESSENGER = 2;
    public static final int MONITOR = 3;
    public static final int PLAYER = 4;
    public static final int AMMO = 5;

    public static final int GEM = 7;
    public static final int KEY = 8;
    public static final int DOOR = 9;
    public static final int SCROLL = 10;
    public static final int PASSAGE = 11;
    public static final int DUPLICATOR = 12;
    public static final int BOMB = 13;
    public static final int ENERGIZER = 14;

    public static final int CLOCKWISE = 16;
    public static final int COUNTER = 17;

    public static final int FOREST = 20;
    public static final int SOLID = 21;
    public static final int NORMAL = 22;
    public static final int BREAKABLE = 23;
    public static final int BOULDER = 24;
    public static final int SLIDERNS = 25;
    public static final int SLIDEREW = 26;
    public static final int FAKE = 27;
    public static final int INVISIBLE = 28;
    public static final int BLINKWALL = 29;
    public static final int TRANSPORTER = 30;
    public static final int LINE = 31;
    public static final int RICOCHET = 32;

    public static final int BEAR = 34;
    public static final int RUFFIAN = 35;
    public static final int OBJECT = 36;
    public static final int SLIME = 37;

    public static final int SPINNINGGUN = 39;
    public static final int PUSHER = 40;
    public static final int LION = 41;
    public static final int TIGER = 42;

    public static final int HEAD = 44;
    public static final int SEGMENT = 45;
}
