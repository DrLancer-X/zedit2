package zedit2;

public class ConvObj implements Comparable<ConvObj> {
    private double rmseImprovement;
    private int x, y;

    public ConvObj(double rmseImprovement, int x, int y) {
        this.rmseImprovement = rmseImprovement;
        this.x = x;
        this.y = y;
    }

    public double getRmseImprovement() {
        return rmseImprovement;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public int compareTo(ConvObj o) {
        if (rmseImprovement < o.rmseImprovement) return 1;
        else if (rmseImprovement > o.rmseImprovement) return -1;
        else if (y < o.y) return -1;
        else if (y > o.y) return 1;
        else if (x < o.x) return -1;
        else if (x > o.x) return 1;
        else return 0;
    }
}
