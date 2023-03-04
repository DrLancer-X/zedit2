package zedit2;

public class SZZTWorldData extends WorldData {
    private static final int WORLD_HEADER_SIZE = 1024;

    public SZZTWorldData(byte[] data) {
        super(data);
    }

    @Override
    public boolean isSuperZZT() {
        return true;
    }
    @Override
    public byte[] getName()
    {
        return Util.readPascalString(data[27], data, 28, 48);
    }
    @Override
    public void setName(byte[] str)
    {
        Util.writePascalString(str, data, 27, 28, 48);
        setDirty(true);
    }
    @Override
    public int getTorches() {
        throw new UnsupportedOperationException("Cannot be used on Super ZZT worlds");
    }
    @Override
    public void setTorches(int val) {
        throw new UnsupportedOperationException("Cannot be used on Super ZZT worlds");
    }
    @Override
    public int getTorchTimer() {
        throw new UnsupportedOperationException("Cannot be used on Super ZZT worlds");
    }
    @Override
    public void setTorchTimer(int val) {
        throw new UnsupportedOperationException("Cannot be used on Super ZZT worlds");
    }
    @Override
    public int getEnergiser() {
        return Util.getInt16(data, 21);
    }
    @Override
    public void setEnergiser(int val) {
        Util.setInt16(data, 21, val);
        setDirty(true);
    }
    @Override
    public int getScore() {
        return Util.getInt16(data, 25);
    }
    @Override
    public void setScore(int val) {
        Util.setInt16(data, 25, val);
        setDirty(true);
    }
    @Override
    public int getTimeSeconds() {
        return Util.getInt16(data, 384);
    }
    @Override
    public void setTimeSeconds(int val) {
        Util.setInt16(data, 384, val);
        setDirty(true);
    }
    @Override
    public int getTimeTicks() {
        return Util.getInt16(data, 386);
    }
    @Override
    public void setTimeTicks(int val) {
        Util.setInt16(data, 386, val);
        setDirty(true);
    }
    @Override
    public boolean getLocked() {
        return data[388] != 0;
    }
    @Override
    public void setLocked(boolean isLocked) {
        data[388] = isLocked ? (byte)1 : (byte)0;
        setDirty(true);
    }
    @Override
    public int getZ() {
        return Util.getInt16(data, 389);
    }
    @Override
    public void setZ(int val) {
        Util.setInt16(data, 389, val);
        setDirty(true);
    }
    @Override
    public int getNumFlags() { return 16; }
    @Override
    public byte[] getFlag(int flag)
    {
        if (flag < 0 || flag >= getNumFlags()) throw new IndexOutOfBoundsException("Invalid flag index");
        int flagOffset = 48 + (21 * flag);
        return Util.readPascalString(data[flagOffset], data, flagOffset + 1, flagOffset + 21);
    }
    @Override
    public void setFlag(int flag, byte[] str)
    {
        if (flag < 0 || flag >= getNumFlags()) throw new IndexOutOfBoundsException("Invalid flag index");
        int flagOffset = 48 + (21 * flag);
        Util.writePascalString(str, data, flagOffset, flagOffset + 1, flagOffset + 21);
        setDirty(true);
    }
    @Override
    protected int boardListOffset() { return WORLD_HEADER_SIZE; }

    @Override
    public Board getBoard(int boardIdx) throws WorldCorruptedException {
        return new SZZTBoard(data, findBoardOffset(boardIdx));
    }

    public static SZZTWorldData createWorld() {
        var bytes = new byte[WORLD_HEADER_SIZE];
        Util.setInt16(bytes, 0, -2);

        var world = new SZZTWorldData(bytes);
        world.setNumBoards(-1);
        world.setHealth(100);
        world.setZ(-1);
        CompatWarning w = new CompatWarning(true);
        world.setBoard(w, 0, new SZZTBoard("Title screen"));
        world.setBoard(w, 1, new SZZTBoard("First board"));
        world.setCurrentBoard(1);
        world.setDirty(false);
        return world;
    }
}
