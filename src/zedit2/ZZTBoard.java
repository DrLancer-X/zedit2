package zedit2;

import java.util.Arrays;

public class ZZTBoard extends Board {
    private boolean dark = false;
    private byte[] message = new byte[0];

    private static final int BOARD_W = 60;
    private static final int BOARD_H = 25;

    public ZZTBoard(byte[] worldData, int boardOffset) throws WorldCorruptedException {

        super();
        var boardName = Util.readPascalString(worldData[boardOffset + 2], worldData, boardOffset + 3, boardOffset + 53);

        setName(boardName);
        var boardPropertiesOffset = decodeRLE(worldData, boardOffset + 53);

        setShots(Util.getInt8(worldData, boardPropertiesOffset + 0));
        setDark(worldData[boardPropertiesOffset + 1] != 0);
        setExit(0, Util.getInt8(worldData, boardPropertiesOffset + 2));
        setExit(1, Util.getInt8(worldData, boardPropertiesOffset + 3));
        setExit(2, Util.getInt8(worldData, boardPropertiesOffset + 4));
        setExit(3, Util.getInt8(worldData, boardPropertiesOffset + 5));
        setRestartOnZap(worldData[boardPropertiesOffset + 6] == 1);

        var boardMsg = Util.readPascalString(worldData[boardPropertiesOffset + 7], worldData,
                boardPropertiesOffset + 8, boardPropertiesOffset + 66);
        setMessage(boardMsg);

        setPlayerX(Util.getInt8(worldData, boardPropertiesOffset + 66));
        setPlayerY(Util.getInt8(worldData, boardPropertiesOffset + 67));
        setTimeLimit((short) Util.getInt16(worldData, boardPropertiesOffset + 68));
        setStats(worldData, boardPropertiesOffset + 86, 8);

        clearDirty();
    }

    public ZZTBoard() {}

    public ZZTBoard(String name) {
        initialise();
        setName(CP437.toBytes(name));
        setShots(255);
        Stat playerStat = new Stat(false);
        playerStat.setCycle(1);
        playerStat.setIsPlayer(true);
        Tile player = new Tile(ZType.PLAYER, 0x1F, playerStat);
        setTile(29, 11, player);
    }

    @Override
    public void write(CompatWarning warning, byte[] worldData, int boardOffset) {
        int boardSize = getCurrentSize();
        Util.setInt16(worldData, boardOffset, boardSize - 2);
        if (getName().length > 50) {
            warning.warn(1, "has a name that is >50 characters and will be truncated.");
        }
        Util.writePascalString(getName(), worldData, boardOffset + 2, boardOffset + 3, boardOffset + 53);
        var boardPropertiesOffset = encodeRLE(worldData, boardOffset + 53);
        Util.setInt8(worldData, boardPropertiesOffset + 0, getShots());
        Util.setInt8(worldData, boardPropertiesOffset + 1, isDark() ? 1 : 0);
        Util.setInt8(worldData, boardPropertiesOffset + 2, getExit(0));
        Util.setInt8(worldData, boardPropertiesOffset + 3, getExit(1));
        Util.setInt8(worldData, boardPropertiesOffset + 4, getExit(2));
        Util.setInt8(worldData, boardPropertiesOffset + 5, getExit(3));
        Util.setInt8(worldData, boardPropertiesOffset + 6, isRestartOnZap() ? 1 : 0);
        Util.writePascalString(getMessage(), worldData, boardPropertiesOffset + 7, boardPropertiesOffset + 8, boardPropertiesOffset + 66);
        Util.setInt8(worldData, boardPropertiesOffset + 66, getPlayerX());
        Util.setInt8(worldData, boardPropertiesOffset + 67, getPlayerY());
        Util.setInt16(worldData, boardPropertiesOffset + 68, getTimeLimit());
        writeStats(warning, worldData, boardPropertiesOffset + 86);
    }

    @Override
    public Board clone() {
        ZZTBoard other = new ZZTBoard();
        cloneInto(other);
        other.dark = dark;
        other.message = message.clone();
        return other;
    }

    @Override
    public boolean isSuperZZT() {
        return false;
    }

    @Override
    public int getWidth() {
        return BOARD_W;
    }
    @Override
    public int getHeight() {
        return BOARD_H;
    }
    @Override
    public boolean isDark() {
        return dark;
    }
    @Override
    public void setDark(boolean dark) {
        this.dark = dark;
        setDirty();
    }
    @Override
    public byte[] getMessage() {
        return message;
    }
    @Override
    public void setMessage(byte[] message) {
        this.message = message;
        setDirty();
    }
    @Override
    public int getCameraX() {
        return 0;
    }
    @Override
    public int getCameraY() {
        return 0;
    }
    @Override
    public void setCameraX(int x) { }
    @Override
    public void setCameraY(int y) { }

    @Override
    /**
     * Guess the board's current size (including length short)
     */
    public int getCurrentSize() {
        int size = 53; // header
        size += getRLESize(); // rle data
        size += 88; // board properties
        size += getStatsSize();
        return size;
    }

    @Override
    protected void drawCharacter(byte[] cols, byte[] chars, int pos, int x, int y) {
        cols[pos] = (byte) ZZTType.getColour(this, x, y);
        chars[pos] = (byte) ZZTType.getChar(this, x, y);
    }

    @Override
    public boolean isEqualTo(Board other) {
        if (!(other instanceof ZZTBoard)) return false;
        if (!super.isEqualTo(other)) return false;

        ZZTBoard zztOther = (ZZTBoard)other;
        if (dark != zztOther.dark) return false;
        if (!Arrays.equals(message, zztOther.message)) return false;

        return true;
    }
}
