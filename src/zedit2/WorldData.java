package zedit2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public abstract class WorldData {
    protected byte[] data;
    private boolean dirty = false;
    public static WorldData loadWorld(File file) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        int fmt = Util.getInt16(data, 0);
        if (fmt == -1) return new ZZTWorldData(data);
        if (fmt == -2) return new SZZTWorldData(data);
        throw new RuntimeException("Invalid or corrupted ZZT file.");
    }
    public WorldData(byte[] data) {
        this.data = data;
    }
    public void write(File file) throws IOException {
        Files.write(file.toPath(), data);
    }
    public int getSize() {
        return data.length;
    }

    public abstract boolean isSuperZZT();
    public abstract byte[] getName();
    public abstract void setName(byte[] str);
    public abstract Board getBoard(int boardIdx) throws WorldCorruptedException;
    public abstract int getNumFlags();
    public abstract byte[] getFlag(int flag);
    public abstract void setFlag(int flag, byte[] str);
    protected abstract int boardListOffset();

    public int getNumBoards() {
        return Util.getInt16(data, 2);
    }
    public void setNumBoards(int val) {
        Util.setInt16(data, 2, val);
        setDirty(true);
    }
    public int getAmmo() {
        return Util.getInt16(data, 4);
    }
    public void setAmmo(int val) {
        Util.setInt16(data, 4, val);
        setDirty(true);
    }
    public int getGems() {
        return Util.getInt16(data, 6);
    }
    public void setGems(int val) {
        Util.setInt16(data, 6, val);
        setDirty(true);
    }
    public int getHealth() {
        return Util.getInt16(data, 15);
    }
    public void setHealth(int val) {
        Util.setInt16(data, 15, val);
        setDirty(true);
    }
    public int getCurrentBoard() {
        return Util.getInt16(data, 17);
    }
    public void setCurrentBoard(int val) {
        Util.setInt16(data, 17, val);
        setDirty(true);
    }
    public boolean getKey(int keyIdx) {
        if (keyIdx < 0 || keyIdx > 6) throw new IndexOutOfBoundsException("Invalid key value");
        return data[keyIdx + 8] != 0;
    }
    public void setKey(int keyIdx, boolean haveKey) {
        if (keyIdx < 0 || keyIdx > 6) throw new IndexOutOfBoundsException("Invalid key value");
        data[keyIdx + 8] = haveKey ? (byte)1 : (byte)0;
        setDirty(true);
    }
    public abstract int getTorches();
    public abstract void setTorches(int val);
    public abstract int getTorchTimer();
    public abstract void setTorchTimer(int val);
    public abstract int getEnergiser();
    public abstract void setEnergiser(int val);
    public abstract int getScore();
    public abstract void setScore(int val);
    public abstract int getTimeSeconds();
    public abstract void setTimeSeconds(int val);
    public abstract int getTimeTicks();
    public abstract void setTimeTicks(int val);
    public abstract boolean getLocked();
    public abstract void setLocked(boolean isLocked);
    public abstract int getZ();
    public abstract void setZ(int val);

    protected int findBoardOffset(int boardIdx) {
        if (boardIdx < 0 || boardIdx > (getNumBoards() + 1)) throw new IndexOutOfBoundsException("Invalid board index of " + boardIdx + ". Must be 0 <= boardIdx <= " + (getNumBoards() + 1));
        int currentOffset = boardListOffset();
        for (int i = 0; i < boardIdx; i++) {
            int boardSize = Util.getUInt16(data, currentOffset);
            currentOffset += boardSize + 2;
        }
        return currentOffset;
    }

    public void setBoard(CompatWarning warning, int boardIdx, Board board) {
        int currentOffset = findBoardOffset(boardIdx);

        int currentBoardSize;
        if (data.length == currentOffset) {
            currentBoardSize = 0;
        } else {
            currentBoardSize = Util.getUInt16(data, currentOffset) + 2;
        }
        int newBoardSize = board.getCurrentSize();
        if (newBoardSize > 65535) {
            warning.warn(2, "is over 65535 bytes and cannot be saved in the ZZT format.");
            return;
        } else if (newBoardSize > 32767) {
            warning.warn(1, "is over 32767 bytes and will require a limitation-removing port to load properly.");
        } else if (newBoardSize > 20000) {
            warning.warn(1, "is over 20000 bytes and may cause memory problems in ZZT.");
        }

        int currentBoardAfter = currentBoardSize + currentOffset;
        int newBoardAfter = newBoardSize + currentOffset;
        // We want to copy everything from currentBoardAfter on to newBoardAfter
        byte[] newData = new byte[data.length - currentBoardSize + newBoardSize];

        // Copy everything up to the board over
        System.arraycopy(data, 0, newData, 0, currentOffset);
        // Write the new board
        board.write(warning, newData, currentOffset);
        // Copy everything from after the board over
        int lengthAfterBoard = data.length - currentBoardAfter;
        System.arraycopy(data, currentBoardAfter, newData, newBoardAfter, lengthAfterBoard);

        data = newData;

        if (boardIdx > getNumBoards()) {
            setNumBoards(boardIdx);
        }
    }

    public void terminateWorld(int boardIdx) {
        int currentOffset = findBoardOffset(boardIdx);
        data = Arrays.copyOfRange(data, 0, currentOffset);
        setNumBoards(boardIdx - 1);
    }

    public WorldData clone() {
        WorldData newWorld;
        if (isSuperZZT()) {
            newWorld = new SZZTWorldData(data.clone());
        } else {
            newWorld = new ZZTWorldData(data.clone());
        }
        newWorld.setDirty(isDirty());
        return newWorld;
    }

    public void setDirty(boolean state) {
        dirty = state;
    }
    public boolean isDirty() {
        return dirty;
    }
}
