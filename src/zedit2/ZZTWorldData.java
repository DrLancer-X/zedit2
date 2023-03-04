package zedit2;

public class ZZTWorldData extends WorldData {
    private static final int WORLD_HEADER_SIZE = 512;
    public ZZTWorldData(byte[] data) {
        super(data);
    }

    @Override
    public boolean isSuperZZT() {
        return false;
    }

    @Override
    public byte[] getName()
    {
        return Util.readPascalString(data[29], data, 30, 50);
    }

    @Override
    public void setName(byte[] str)
    {
        Util.writePascalString(str, data, 29, 30, 50);
        setDirty(true);
    }

    @Override
    public int getTorches() {
        return Util.getInt16(data, 19);
    }
    @Override
    public void setTorches(int val) {
        Util.setInt16(data, 19, val);
        setDirty(true);
    }
    @Override
    public int getTorchTimer() {
        return Util.getInt16(data, 21);
    }
    @Override
    public void setTorchTimer(int val) {
        Util.setInt16(data, 21, val);
        setDirty(true);
    }
    @Override
    public int getEnergiser() {
        return Util.getInt16(data, 23);
    }
    @Override
    public void setEnergiser(int val) {
        Util.setInt16(data, 23, val);
        setDirty(true);
    }
    @Override
    public int getScore() {
        return Util.getInt16(data, 27);
    }
    @Override
    public void setScore(int val) {
        Util.setInt16(data, 27, val);
        setDirty(true);
    }
    @Override
    public int getTimeSeconds() {
        return Util.getInt16(data, 260);
    }
    @Override
    public void setTimeSeconds(int val) {
        Util.setInt16(data, 260, val);
        setDirty(true);
    }
    @Override
    public int getTimeTicks() {
        return Util.getInt16(data, 262);
    }
    @Override
    public void setTimeTicks(int val) {
        Util.setInt16(data, 262, val);
        setDirty(true);
    }
    @Override
    public boolean getLocked() {
        return data[264] != 0;
    }
    @Override
    public void setLocked(boolean isLocked) {
        data[264] = isLocked ? (byte)1 : (byte)0;
        setDirty(true);
    }
    @Override
    public int getZ() {
        throw new UnsupportedOperationException("Cannot be used on ZZT worlds");
    }
    @Override
    public void setZ(int val) {
        throw new UnsupportedOperationException("Cannot be used on ZZT worlds");
    }
    @Override
    public int getNumFlags() { return 10; }
    @Override
    public byte[] getFlag(int flag)
    {
        if (flag < 0 || flag >= getNumFlags()) throw new IndexOutOfBoundsException("Invalid flag index");
        int flagOffset = 50 + (21 * flag);
        return Util.readPascalString(data[flagOffset], data, flagOffset + 1, flagOffset + 21);
    }
    @Override
    public void setFlag(int flag, byte[] str)
    {
        if (flag < 0 || flag >= getNumFlags()) throw new IndexOutOfBoundsException("Invalid flag index");
        int flagOffset = 50 + (21 * flag);
        Util.writePascalString(str, data, flagOffset, flagOffset + 1, flagOffset + 21);
        setDirty(true);
    }
    @Override
    protected int boardListOffset() { return WORLD_HEADER_SIZE; }

    @Override
    public Board getBoard(int boardIdx) throws WorldCorruptedException {
        return new ZZTBoard(data, findBoardOffset(boardIdx));
    }

    public static ZZTWorldData createWorld() {
        var bytes = new byte[WORLD_HEADER_SIZE];
        Util.setInt16(bytes, 0, -1);

        var world = new ZZTWorldData(bytes);
        world.setNumBoards(-1);
        world.setHealth(100);
        CompatWarning w = new CompatWarning(false);
        world.setBoard(w, 0, new ZZTBoard("Title screen"));
        world.setBoard(w, 1, new ZZTBoard("First board"));
        world.setCurrentBoard(1);
        world.setDirty(false);
        return world;
    }
}
