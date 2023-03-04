package zedit2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public abstract class Board {
    private boolean isCorrupted = false;
    private int[] bid, bco;
    private byte[] name;
    private int shots;
    private int[] exits = new int[4];
    private boolean restartOnZap;
    private int playerX, playerY;
    private int timeLimit;
    private ArrayList<Stat> stats = new ArrayList<>();
    private int rleSizeSaved = 0;
    private int statsSizeSaved = 0;
    private boolean dirty = false;
    private boolean needsFinalisation = false;
    private long dirtyTimestamp = 0;

    public abstract boolean isSuperZZT();
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract boolean isDark();
    public abstract void setDark(boolean dark);
    public abstract byte[] getMessage();
    public abstract void setMessage(byte[] message);
    public abstract int getCameraX();
    public abstract int getCameraY();
    public abstract void setCameraX(int x);
    public abstract void setCameraY(int y);

    public boolean isCorrupted() {
        return isCorrupted;
    }
    public void setCorrupted() {
        isCorrupted = true;
    }

    protected void initialise() {
        int w = getWidth();
        int h = getHeight();
        bid = new int[w * h];
        bco = new int[w * h];
    }

    protected int decodeRLE(byte[] worldData, int dataOffset) throws WorldCorruptedException
    {
        initialise();

        int w = getWidth();
        int h = getHeight();

        int pos = 0;
        try {
            while (pos < w * h) {
                int len = Util.getInt8(worldData, dataOffset);
                if (len == 0) len = 256;
                for (int i = 0; i < len; i++) {
                    bid[pos] = Util.getInt8(worldData, dataOffset + 1);
                    bco[pos] = Util.getInt8(worldData, dataOffset + 2);
                    pos++;
                }
                dataOffset += 3;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new WorldCorruptedException("RLE decoding error");
        }
        dirtyRLE();
        return dataOffset;
    }

    protected int encodeRLE(byte[] worldData, int dataOffset)
    {
        int w = getWidth();
        int h = getHeight();
        int runPos = dataOffset;
        // Write our initial run (0, 0, 0)
        Util.setInt8(worldData, runPos + 0, 0);
        Util.setInt8(worldData, runPos + 1, 0);
        Util.setInt8(worldData, runPos + 2, 0);

        for (int i = 0; i < w * h; i++) {
            int curr_bid = bid[i] & 0xFF;
            int curr_bco = bco[i] & 0xFF;

            int run_len = Util.getInt8(worldData, runPos + 0);
            int run_bid = Util.getInt8(worldData, runPos + 1);
            int run_bco = Util.getInt8(worldData, runPos + 2);
            if (curr_bid == run_bid && curr_bco == run_bco && run_len < 255) {
                Util.setInt8(worldData, runPos + 0, Util.getInt8(worldData, runPos + 0) + 1);
            } else {
                if (run_len != 0) runPos += 3;
                Util.setInt8(worldData, runPos + 0, 1);
                Util.setInt8(worldData, runPos + 1, curr_bid);
                Util.setInt8(worldData, runPos + 2, curr_bco);
            }
        }
        runPos += 3;
        return runPos;
    }

    public void dirtyRLE() {
        rleSizeSaved = 0;
        setDirty();
    }

    public void dirtyStats() {
        statsSizeSaved = 0;
        setDirty();
    }

    protected int getRLESize() {
        if (rleSizeSaved == 0) {
            int w = getWidth();
            int h = getHeight();
            int runs = 0;
            int run_bid = -1;
            int run_bco = -1;
            int run_len = 0;

            for (int i = 0; i < w * h; i++) {
                int curr_bid = bid[i] & 0xFF;
                int curr_bco = bco[i] & 0xFF;
                if (curr_bid == run_bid && curr_bco == run_bco && run_len < 255) {
                    run_len++;
                } else {
                    runs++;
                    run_bid = curr_bid;
                    run_bco = curr_bco;
                    run_len = 1;
                }
            }
            rleSizeSaved = runs * 3;
        }
        return rleSizeSaved;
    }

    protected int getStatsSize() {
        finalisationCheck();
        if (statsSizeSaved == 0) {
            for (var stat : stats) {
                statsSizeSaved += stat.getStatSize();
            }
        }
        return statsSizeSaved;
    }

    public abstract int getCurrentSize();

    public byte[] getName() {
        return name;
    }

    public void setName(byte[] name) {
        this.name = name;
        setDirty();
    }

    public int getShots() {
        return shots;
    }

    public void setShots(int shots) {
        this.shots = shots;
        setDirty();
    }

    public int getExit(int exit) {
        return exits[exit];
    }

    public void setExit(int exit, int val) {
        exits[exit] = val;
        setDirty();
    }

    public boolean isRestartOnZap() {
        return restartOnZap;
    }

    public void setRestartOnZap(boolean restartOnZap) {
        this.restartOnZap = restartOnZap;
        setDirty();
    }

    public int getPlayerX() {
        return playerX;
    }

    public void setPlayerX(int x) {
        this.playerX = x;
        setDirty();
    }

    public int getPlayerY() {
        return playerY;
    }

    public void setPlayerY(int y) {
        this.playerY = y;
        setDirty();
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
        setDirty();
    }

    public int getStatCount() {
        return stats.size();
    }

    public void setStats(byte[] worldData, int offset, int padding) {
        int statCount = Util.getInt16(worldData, offset);
        offset += 2;
        stats.clear();

        for (int i = 0; i <= statCount; i++) {
            var stat = new Stat(worldData, offset, padding, i);
            stat.setIsPlayer(i == 0);
            offset += stat.getStatSize();
            stats.add(stat);
        }
    }

    protected int writeStats(CompatWarning warning, byte[] worldData, int offset) {
        finalisationCheck();

        if (warning.isSuperZZT()) {
            if (stats.size() - 1 > 128) {
                warning.warn(1, "has over 128 stats, which may cause problems in Super ZZT.");
            }
        } else {
            if (stats.size() - 1 > 150) {
                warning.warn(1, "has over 150 stats, which may cause problems in ZZT.");
            }
        }
        Util.setInt16(worldData, offset, stats.size() - 1);
        offset += 2;

        for (var stat : stats) {
            offset = stat.write(worldData, offset);
        }
        return offset;
    }

    protected abstract void drawCharacter(byte[] cols, byte[] chars, int pos, int x, int y);

    public int getTileCol(int x, int y)
    {
        return bco[y * getWidth() + x] & 0xFF;
    }

    public int getTileId(int x, int y)
    {
        return bid[y * getWidth() + x] & 0xFF;
    }

    public List<Stat> getStatsAt(int x, int y)
    {
        var tileStats = new ArrayList<Stat>();
        for (Stat stat : stats) {
            if (stat.getX() == x + 1 && stat.getY() == y + 1) {
                tileStats.add(stat.clone());
            }
        }
        return tileStats;
    }

    public Tile getTile(int x, int y, boolean copy) {
        Tile tile = new Tile(getTileId(x, y), getTileCol(x, y), getStatsAt(x, y));
        if (copy) {
            for (var stat : tile.getStats()) {
                var visited = new HashSet<Stat>();
                var followStat = stat;
                for (;;) {
                    if (visited.contains(followStat)) {
                        // Failed to follow, remove the bind
                        stat.setCodeLength(0);
                        break;
                    }
                    visited.add(followStat);
                    if (followStat.getCodeLength() >= 0) {
                        // Now we are at the parent. We will steal its code, then set autobind on
                        if (followStat != stat) {
                            stat.setCode(followStat.getCode());
                            stat.setAutobind(true);
                        }
                        break;
                    } else {
                        int newId = followStat.getCodeLength() * -1;
                        if (newId < stats.size()) {
                            followStat = stats.get(newId);
                        } else {
                            // Failed to follow, remove the bind
                            stat.setCodeLength(0);
                        }
                    }
                }
            }
        }
        return tile;
    }

    public void setTile(int x, int y, Tile tile) {
        setTileDirect(x, y, tile);
        finaliseStats();
    }
    public void setTileDirect(int x, int y, Tile tile) {
        int pos = y * getWidth() + x;
        bid[pos] = (byte) tile.getId();
        bco[pos] = (byte) tile.getCol();
        dirtyRLE();

        var statsInTile = new HashMap<Integer, Stat>();
        var statsToPut = new HashSet<Stat>();
        if (tile.getStats() != null) {
            for (var stat : tile.getStats()) {
                statsToPut.add(stat);
                if (stat.getStatId() != -1) {
                    statsInTile.put(stat.getStatId(), stat);
                }
            }
        }

        var statsToDelete = new ArrayList<Integer>();
        for (int i = 0; i < stats.size(); i++) {
            var stat = stats.get(i);
            if (stat.getX() == x + 1 && stat.getY() == y + 1) {
                if (statsInTile.containsKey(stat.getStatId())) {
                    var newStat = statsInTile.get(stat.getStatId());
                    statsInTile.remove(stat.getStatId());
                    statsToPut.remove(newStat);
                    stats.set(i, newStat);
                } else {
                    statsToDelete.add(i);
                }
            }
        }

        directDeleteStats(statsToDelete);

        for (var stat : statsToPut) {
            var newStat = stat.clone();
            newStat.setX(x + 1);
            newStat.setY(y + 1);
            newStat.setStatId(-1);
            stats.add(newStat);
        }

        needsFinalisation = true;
    }

    public void directAddStat(Stat stat) {
        stats.add(stat.clone());
        needsFinalisation = true;
    }

    public boolean directReplaceStat(int statId, Stat replacementStat) {
        if (stats.get(statId).isIdenticalTo(replacementStat)) return false;
        stats.set(statId, replacementStat.clone());
        needsFinalisation = true;
        return true;
    }

    public boolean directDeleteStats(List<Integer> statsToDelete) {
        if (statsToDelete.isEmpty()) return false;
        int offset = 0;
        int lastValue = -1;
        for (int statIdx : statsToDelete) {
            if (statIdx <= lastValue) {
                throw new RuntimeException("Stat list must be unique and ordered");
            }
            statDeleted(statIdx - offset);
            stats.remove(statIdx - offset);
            offset++;
        }
        needsFinalisation = true;
        return true;
    }

    public void finaliseStats() {
        postProcessStats();
        needsFinalisation = false;
    }

    private void finalisationCheck() {
        if (needsFinalisation) {
            throw new RuntimeException("finaliseStats not called after direct modifications");
        }
    }

    /**
     * Sets a tile without affecting stats
     */
    public void setTileRaw(int x, int y, int id, int col) {
        int pos = y * getWidth() + x;
        if (bid[pos] != (byte) id || bco[pos] != (byte) col) {
            bid[pos] = (byte) id;
            bco[pos] = (byte) col;
            dirtyRLE();
        }
    }
    private void postProcessStats() {
        if (stats.size() == 0) return;

        var newStatList = new Stat[stats.size()];
        var placed = new boolean[stats.size()];
        // stat 0 always goes in the same place
        newStatList[0] = stats.get(0);

        placed[0] = true;

        // Go through the stats with specific IDs
        for (int i = 1; i < stats.size(); i++) {
            var stat = stats.get(i);
            if (stat.isSpecifyId()) {
                var preferredPos = stat.getOrder();
                if (preferredPos >= 1 && preferredPos < newStatList.length) {
                    if (newStatList[preferredPos] == null) {
                        newStatList[preferredPos] = stat;
                        placed[i] = true;
                    }
                }
            }
        }

        for (int i = 1; i < stats.size(); i++) {
            if (placed[i]) continue;
            var stat = stats.get(i);
            if (stat.isSpecifyId()) {
                var preferredPos = Util.clamp(stat.getOrder(), 1, newStatList.length - 1);
                int dir = 1;
                for (;;) {
                    if (newStatList[preferredPos] == null) {
                        newStatList[preferredPos] = stat;
                        placed[i] = true;
                        break;
                    }
                    preferredPos += dir;
                    if (preferredPos >= newStatList.length) {
                        dir *= -1;
                        preferredPos += dir * 2;
                    }
                }
            }
        }

        ArrayList<Stat> notPlaced = new ArrayList<>();

        for (int i = 1; i < stats.size(); i++) {
            if (placed[i]) continue;
            var stat = stats.get(i);
            notPlaced.add(stat);
        }
        notPlaced.sort(Comparator.comparingInt(Stat::getOrder));
        int notPlacedIdx = 0;
        for (int preferredPos = 1; preferredPos < newStatList.length; preferredPos++) {
            if (newStatList[preferredPos] != null) continue;
            var stat = notPlaced.get(notPlacedIdx);
            newStatList[preferredPos] = stat;
            notPlacedIdx++;
        }

        HashMap<Integer, Integer> oldToNew = new HashMap<>();
        HashMap<Integer, Integer> newToOld = new HashMap<>();
        for (int i = 0; i < stats.size(); i++) {
            int newIdx = i;
            int oldIdx = newStatList[i].getStatId();
            if (oldIdx != -1) {
                oldToNew.put(oldIdx, newIdx);
                newToOld.put(newIdx, oldIdx);
            }

            stats.set(i, newStatList[i]);
            stats.get(i).setStatId(i);
        }

        // Remapping stage
        for (Stat stat : stats) {
            if (stat.getFollower() != -1) {
                Integer newFollower = oldToNew.get(stat.getFollower());
                if (newFollower == null) stat.setFollower(-1);
                else stat.setFollower(newFollower);
            }
            if (stat.getLeader() != -1) {
                Integer newLeader = oldToNew.get(stat.getLeader());
                if (newLeader == null) stat.setLeader(-1);
                else stat.setLeader(newLeader);
            }
            if (stat.getCodeLength() < 0) {
                Integer newBind = oldToNew.get(-stat.getCodeLength());
                if (newBind == null) {
                    stat.setCodeLength(0);
                } else {
                    stat.setCodeLength(-newBind);
                }
            }
        }
        // Rebinding stage
        var bindMap = new HashMap<Integer, ArrayList<Integer>>();
        var followedSet = new HashSet<Stat>();
        for (int i = 0; i < stats.size(); i++) {
            var stat = stats.get(i);
            Stat followStat = stat;
            int codeOwnerIdx = i;
            for (;;) {
                if (followedSet.contains(followStat)) {
                    codeOwnerIdx = -1;
                    break;
                }
                followedSet.add(followStat);
                if (followStat.getCodeLength() < 0) {
                    int bind = -stat.getCodeLength();
                    if (bind < stats.size()) {
                        codeOwnerIdx = bind;
                        followStat = stats.get(codeOwnerIdx);
                    } else break;
                } else break;
            }
            if (codeOwnerIdx != -1 && codeOwnerIdx != i) {
                if (!bindMap.containsKey(codeOwnerIdx)) {
                    var list = new ArrayList<Integer>();
                    list.add(codeOwnerIdx);
                    bindMap.put(codeOwnerIdx, list);
                }
                var list = bindMap.get(codeOwnerIdx);
                list.add(i);
            }
        }
        for (int codeOwnerIdx : bindMap.keySet()) {
            var list = bindMap.get(codeOwnerIdx);
            list.sort(null);
            var code = stats.get(codeOwnerIdx).getCode();
            int newOwner = list.get(0);
            for (int i : list) {
                System.out.printf("Rebinding %d to %d\n", i, newOwner);
                stats.get(i).setCodeLength(-newOwner);
            }
            stats.get(newOwner).setCode(code);
        }
        // Autobinding stage
        var autoBind = new HashMap<String, Integer>();
        for (int i = 1; i < stats.size(); i++) {
            if (stats.get(i).getCodeLength() > 0) {
                var code = CP437.toUnicode(stats.get(i).getCode());
                if (autoBind.containsKey(code)) {
                    if (stats.get(i).isAutobind()) {
                        // Bind to the earlier object
                        stats.get(i).setCodeLength(autoBind.get(code) * -1);
                        //System.out.printf("stat #%d autobound with #%d\n", i, autoBind.get(code));
                    }
                } else {
                    autoBind.put(code, i);
                }
            }
        }

        dirtyStats();
    }

    public void drawToCanvas(DosCanvas canvas, int offsetX, int offsetY, int x1, int y1, int x2, int y2, int showing)
    {
        finalisationCheck();

        int width = x2 - x1 + 1;
        int height = y2 - y1 + 1;
        //System.out.printf("Draw %d x %d\n", width, height);
        var cols = new byte[width * height];
        var chars = new byte[width * height];
        byte[] show = null;
        if (showing != WorldEditor.SHOW_NOTHING) {
            show = new byte[width * height];

            if (showing == WorldEditor.SHOW_STATS) {
                for (var stat : stats) {
                    int x = stat.getX() - 1 - x1;
                    int y = stat.getY() - 1 - y1;
                    if (x >= 0 && y >= 0 && x < width && y < height) {
                        show[y * width + x] = (byte) showing;
                    }
                }
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pos = y * width + x;
                drawCharacter(cols, chars, pos, x + x1, y + y1);
                if (showing != WorldEditor.SHOW_NOTHING) {
                    int bpos = (y + y1) * getWidth() + x + x1;
                    int id = bid[bpos];
                    switch (showing) {
                        case WorldEditor.SHOW_EMPTIES:
                        case WorldEditor.SHOW_EMPTEXTS:
                            if (id == ZType.EMPTY) {
                                show[pos] = (byte) showing;
                                cols[pos] = (byte) bco[bpos];
                            }
                            break;
                        case WorldEditor.SHOW_FAKES:
                            if (id == ZType.FAKE) show[pos] = (byte) showing;
                            break;
                        case WorldEditor.SHOW_INVISIBLES:
                            if (id == ZType.INVISIBLE) show[pos] = (byte) showing;
                            break;
                        case WorldEditor.SHOW_OBJECTS:
                            if (id == ZType.OBJECT) show[pos] = (byte) showing;
                            break;
                    }
                }
            }
        }
        canvas.setData(width, height, cols, chars, offsetX + x1, offsetY + y1, showing, show);
    }
    /*
    public void drawToCanvasOld(DosCanvas canvas, int offsetX, int offsetY, int x1, int y1, int x2, int y2)
    {
        finalisationCheck();

        int width = getWidth();
        int height = getHeight();
        var cols = new byte[width * height];
        var chars = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pos = y * width + x;
                drawCharacter(cols, chars, x, y);
            }
        }
        canvas.setData(width, height, cols, chars, offsetX, offsetY);
    }
    */
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        dirty = true;
        dirtyTimestamp = GlobalEditor.getTimestamp();
    }

    public void clearDirty() {
        dirty = false;
    }

    public abstract void write(CompatWarning warning, byte[] newData, int currentOffset);

    public Stat getStat(int idx) {
        return stats.get(idx);
    }

    public abstract Board clone();

    protected void cloneInto(Board other) {
        other.isCorrupted = isCorrupted;
        other.bid = bid.clone();
        other.bco = bco.clone();
        other.name = name.clone();
        other.shots = shots;
        other.exits = exits.clone();
        other.restartOnZap = restartOnZap;
        other.playerX = playerX;
        other.playerY = playerY;
        other.timeLimit = timeLimit;

        other.setDark(isDark());
        other.setMessage(getMessage());
        other.setCameraX(getCameraX());
        other.setCameraY(getCameraY());

        other.stats = new ArrayList<>();
        for (Stat stat : stats) other.stats.add(stat.clone());
        other.rleSizeSaved = rleSizeSaved;
        other.statsSizeSaved = statsSizeSaved;
        other.dirty = dirty;
        other.needsFinalisation = needsFinalisation;
        other.dirtyTimestamp = dirtyTimestamp;
    }

    private void statDeleted(int statIdx) {
        //System.out.println("Deleted stat " + statIdx);
        var stat = stats.get(statIdx);
        var statId = stat.getStatId();

        var statPos = new HashMap<Integer, Integer>();
        for (int i = 0; i < stats.size(); i++) {
            statPos.put(stats.get(i).getStatId(), i);
        }

        // First of all, does this stat have its own code or is it just another in the chain?
        int codeOwnerIdx = statIdx;
        int codeOwnerId = statId;
        for (;;) {
            int cl = stats.get(codeOwnerIdx).getCodeLength();
            if (cl >= 0) break; // Reached the parent
            if (-cl >= stats.size()) {
                // This is as far as we can go
                break;
            } else {
                codeOwnerIdx = statPos.get(-cl);
                codeOwnerId = stats.get(codeOwnerIdx).getStatId();
            }
        }

        // Fix binds and erase centipede links
        for (int i = 0; i < stats.size(); i++) {
            if (stats.get(i).getCodeLength() == -statId) {
                // Stat i is bound to statIdx.
                if (codeOwnerIdx == statIdx) {
                    // As statIdx is going away, stat i will become the new owner
                    var targetCodeLen = stats.get(statIdx).getCodeLength();
                    var targetCode = stats.get(statIdx).getCode();
                    stats.get(i).setCode(targetCode);
                    if (targetCodeLen < 0) stats.get(i).setCodeLength(targetCodeLen);
                    codeOwnerIdx = i;
                    codeOwnerId = stats.get(i).getStatId();
                } else {
                    // Otherwise, point to the owner
                    stats.get(i).setCodeLength(-codeOwnerId);
                }
            }
            if (stats.get(i).getFollower() == statId) {
                // Break link
                stats.get(i).setFollower(-1);
            }
            if (stats.get(i).getLeader() == statId) {
                // Break link
                stats.get(i).setLeader(-1);
            }
        }
        dirtyStats();
    }

    public void saveTo(File file) throws IOException {
        CompatWarning w = new CompatWarning(isSuperZZT());
        byte[] data = new byte[getCurrentSize()];
        write(w, data, 0);
        Files.write(file.toPath(), data);
    }

    public void loadFrom(File file) throws IOException, WorldCorruptedException {
        Board loadedBoard;
        var data = Files.readAllBytes(file.toPath());

        if (isSuperZZT()) {
            loadedBoard = new SZZTBoard(data, 0);
        } else {
            loadedBoard = new ZZTBoard(data, 0);
        }
        loadedBoard.cloneInto(this);
        setDirty();
    }

    /**
     * Compare for equality, ignoring 'dirty' flag
     * @param other
     * @return
     */
    public boolean isEqualTo(Board other) {
        if (isCorrupted != other.isCorrupted) return false;
        if (!Arrays.equals(bid, other.bid)) return false;
        if (!Arrays.equals(bco, other.bco)) return false;
        if (!Arrays.equals(name, other.name)) return false;
        if (!Arrays.equals(exits, other.exits)) return false;
        if (restartOnZap != other.restartOnZap) return false;
        if (playerX != other.playerX) return false;
        if (playerY != other.playerY) return false;
        if (timeLimit != other.timeLimit) return false;
        if (stats.size() != other.stats.size()) return false;
        for (int i = 0; i < stats.size(); i++) {
            if (!stats.get(i).isIdenticalTo(other.stats.get(i))) return false;
        }
        return true;
    }

    public boolean timestampEquals(Board other) {
        return dirtyTimestamp == other.dirtyTimestamp;
    }

    public long getDirtyTimestamp() { return dirtyTimestamp; }
}
