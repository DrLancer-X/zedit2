package zedit2;

import java.util.ArrayList;
import java.util.List;

public class Tile {
    private int id, col;
    private ArrayList<Stat> stats;

    public Tile(int id, int col, List<Stat> stats) {
        this(id, col);
        setStats(stats);
    }

    public Tile(int id, int col, Stat stat) {
        this(id, col);
        stats.add(stat.clone());
    }

    public Tile(int id, int col) {
        this.id = id;
        this.col = col;
        stats = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public List<Stat> getStats() {
        return stats;
    }

    public void setStats(List<Stat> stats) {
        this.stats.clear();
        if (stats == null) return;
        for (Stat stat : stats) {
            this.stats.add(stat.clone());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Tile)) return false;
        Tile other = (Tile)obj;
        if (other.id != id || other.col != col) return false;
        return other.stats.equals(stats);
    }

    public void addStat(Stat newStat) {
        if (stats == null) stats = new ArrayList<>();
        stats.add(newStat);
    }
    public void delStat(int idx) {
        stats.remove(idx);
    }

    public Tile clone() {
        Tile tile = new Tile(id, col, stats);
        return tile;
    }
}
