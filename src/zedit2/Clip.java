package zedit2;

import java.util.ArrayList;
import java.util.Base64;

public class Clip {
    private int w, h;
    private Tile[] tiles;
    private boolean szzt;

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public Tile[] getTiles() {
        return tiles;
    }

    public boolean isSzzt() {
        return szzt;
    }

    public static String encode(int w, int h, Tile[] tiles, boolean szzt) {
        // Calculate size
        int size = 9;
        for (Tile tile : tiles) {
            size += tileSize(tile);
        }
        byte[] data = new byte[size];
        Util.setInt32(data, 0, w);
        Util.setInt32(data, 4, h);
        Util.setInt8(data, 8, szzt ? 1 : 0);
        int offset = 9;
        for (var tile : tiles) {
            Util.setInt8(data, offset, tile.getId());
            Util.setInt8(data, offset + 1, tile.getCol());
            var stats = tile.getStats();
            Util.setInt8(data, offset + 2, stats.size());
            offset += 3;
            for (var stat : stats) {
                offset = stat.write(data, offset);
            }
        }
        return Base64.getEncoder().encodeToString(data);
    }
    public static Clip decode(String encodedClip) {
        var data = Base64.getDecoder().decode(encodedClip);
        var clip = new Clip();
        clip.w = Util.getInt32(data, 0);
        clip.h = Util.getInt32(data, 4);
        clip.szzt = Util.getInt8(data, 8) == 1;
        clip.tiles = new Tile[clip.w * clip.h];
        int offset = 9;
        for (int i = 0; i < clip.tiles.length; i++) {
            int id, col, statCount;
            id = Util.getInt8(data, offset);
            col = Util.getInt8(data, offset + 1);
            statCount = Util.getInt8(data, offset + 2);
            offset += 3;
            ArrayList<Stat> stats = new ArrayList<>(statCount);
            for (int j = 0; j < statCount; j++) {
                Stat stat = new Stat(data, offset, clip.szzt ? 0 : 8, -1);
                offset += stat.getStatSize();
                stats.add(stat);
            }
            clip.tiles[i] = new Tile(id, col, stats);
        }
        return clip;
    }

    private static int tileSize(Tile tile) {
        int size = 3; // id, col and stat
        var stats = tile.getStats();
        for (var stat : stats) {
            size += stat.getStatSize();
        }
        return size;
    }
}
