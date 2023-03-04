package zedit2;

public class BufferBoard extends Board {
    private boolean szzt;
    int w, h;
    public BufferBoard(boolean szzt, int w, int h) {
        this.szzt = szzt;
        this.w = w;
        this.h = h;
        initialise();
    }

    @Override
    public boolean isSuperZZT() {
        return szzt;
    }

    @Override
    public int getWidth() {
        return w;
    }

    @Override
    public int getHeight() {
        return h;
    }

    @Override
    public boolean isDark() {
        return false;
    }

    @Override
    public void setDark(boolean dark) { }

    @Override
    public byte[] getMessage() { return new byte[0]; }

    @Override
    public void setMessage(byte[] message) { }

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
    public int getCurrentSize() {
        throw new UnsupportedOperationException("Invalid for buffer boards");
    }

    @Override
    protected void drawCharacter(byte[] cols, byte[] chars, int pos, int x, int y) {
        if (!szzt) {
            cols[pos] = (byte) ZZTType.getColour(this, x, y);
            chars[pos] = (byte) ZZTType.getChar(this, x, y);
        } else {
            cols[pos] = (byte) SZZTType.getColour(this, x, y);
            chars[pos] = (byte) SZZTType.getChar(this, x, y);
        }
    }

    @Override
    public void write(CompatWarning warning, byte[] newData, int currentOffset) {
        throw new UnsupportedOperationException("Invalid for buffer boards");
    }

    @Override
    public Board clone() {
        Board other = new BufferBoard(szzt, w, h);
        cloneInto(other);
        return other;
    }

    @Override
    public boolean isEqualTo(Board other) {
        if (!(other instanceof BufferBoard)) return false;
        if (!super.isEqualTo(other)) return false;

        BufferBoard bufOther = (BufferBoard)other;
        if (w != bufOther.w) return false;
        if (h != bufOther.h) return false;
        if (szzt != bufOther.szzt) return false;

        return true;
    }
}
