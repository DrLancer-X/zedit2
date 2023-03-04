package zedit2;

import java.util.Arrays;

public class SZZTBoard extends Board {
    private int cameraX, cameraY;

    private static final int BOARD_W = 96;
    private static final int BOARD_H = 80;

    public SZZTBoard(byte[] worldData, int boardOffset) throws WorldCorruptedException {

        super();
        var boardName = Util.readPascalString(worldData[boardOffset + 2], worldData,
                boardOffset + 3, boardOffset + 63);

        setName(boardName);
        var boardPropertiesOffset = decodeRLE(worldData, boardOffset + 63);

        setShots(Util.getInt8(worldData, boardPropertiesOffset + 0));
        setExit(0, Util.getInt8(worldData, boardPropertiesOffset + 1));
        setExit(1, Util.getInt8(worldData, boardPropertiesOffset + 2));
        setExit(2, Util.getInt8(worldData, boardPropertiesOffset + 3));
        setExit(3, Util.getInt8(worldData, boardPropertiesOffset + 4));
        setRestartOnZap(worldData[boardPropertiesOffset + 5] == 1);

        setPlayerX(Util.getInt8(worldData, boardPropertiesOffset + 6));
        setPlayerY(Util.getInt8(worldData, boardPropertiesOffset + 7));
        setCameraX(Util.getInt16(worldData, boardPropertiesOffset + 8));
        setCameraY(Util.getInt16(worldData, boardPropertiesOffset + 10));
        setTimeLimit((short) Util.getInt16(worldData, boardPropertiesOffset + 12));
        setStats(worldData, boardPropertiesOffset + 28, 0);

        clearDirty();
    }
    public SZZTBoard() {}

    public SZZTBoard(String name) {
        initialise();
        setName(CP437.toBytes(name));
        setShots(255);
        Stat playerStat = new Stat(true);
        playerStat.setCycle(1);
        playerStat.setIsPlayer(true);
        Tile player = new Tile(ZType.PLAYER, 0x1F, playerStat);
        setTile(48, 40, player);
    }

    @Override
    public void write(CompatWarning warning, byte[] worldData, int boardOffset) {
        int boardSize = getCurrentSize();
        Util.setInt16(worldData, boardOffset, boardSize - 2);
        if (getName().length > 60) {
            warning.warn(1, "has a name that is >60 characters and will be truncated.");
        }
        Util.writePascalString(getName(), worldData, boardOffset + 2, boardOffset + 3, boardOffset + 63);
        var boardPropertiesOffset = encodeRLE(worldData, boardOffset + 63);
        Util.setInt8(worldData, boardPropertiesOffset + 0, getShots());
        Util.setInt8(worldData, boardPropertiesOffset + 1, getExit(0));
        Util.setInt8(worldData, boardPropertiesOffset + 2, getExit(1));
        Util.setInt8(worldData, boardPropertiesOffset + 3, getExit(2));
        Util.setInt8(worldData, boardPropertiesOffset + 4, getExit(3));
        Util.setInt8(worldData, boardPropertiesOffset + 5, isRestartOnZap() ? 1 : 0);
        Util.setInt8(worldData, boardPropertiesOffset + 6, getPlayerX());
        Util.setInt8(worldData, boardPropertiesOffset + 7, getPlayerY());
        Util.setInt16(worldData, boardPropertiesOffset + 8, getCameraX());
        Util.setInt16(worldData, boardPropertiesOffset + 10, getCameraY());
        Util.setInt16(worldData, boardPropertiesOffset + 12, getTimeLimit());
        writeStats(warning, worldData, boardPropertiesOffset + 28);
    }

    @Override
    public Board clone() {
        SZZTBoard other = new SZZTBoard();
        cloneInto(other);
        other.cameraX = cameraX;
        other.cameraY = cameraY;
        return other;
    }

    @Override
    public boolean isSuperZZT() {
        return true;
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
        return false;
    }
    @Override
    public void setDark(boolean dark) { }
    @Override
    public byte[] getMessage() {
        return new byte[0];
    }
    @Override
    public void setMessage(byte[] message) { }

    @Override
    public int getCameraX() {
        return cameraX;
    }
    @Override
    public int getCameraY() {
        return cameraY;
    }
    @Override
    public void setCameraX(int x) {
        cameraX = x;
        setDirty();
    }
    @Override
    public void setCameraY(int y) {
        cameraY = y;
        setDirty();
    }

    @Override
    public int getCurrentSize() {
        int size = 63; // header
        size += getRLESize(); // rle data
        size += 30; // board properties
        size += getStatsSize();
        return size;
    }

    @Override
    protected void drawCharacter(byte[] cols, byte[] chars, int pos, int x, int y) {
        cols[pos] = (byte) SZZTType.getColour(this, x, y);
        chars[pos] = (byte) SZZTType.getChar(this, x, y);
    }

    @Override
    public boolean isEqualTo(Board other) {
        if (!(other instanceof SZZTBoard)) return false;
        if (!super.isEqualTo(other)) return false;

        SZZTBoard szztOther = (SZZTBoard)other;
        if (cameraX != szztOther.cameraX) return false;
        if (cameraY != szztOther.cameraY) return false;

        return true;
    }
}
