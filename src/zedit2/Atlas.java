package zedit2;

public class Atlas {
    private int w, h;
    private int[][] grid;

    public Atlas(int w, int h, int[][] grid)
    {
        this.w = w;
        this.h = h;
        this.grid = grid;
    }

    public int getW() {
        return w;
    }
    public int getH() {
        return h;
    }
    public int[][] getGrid() {
        return grid;
    }
}
