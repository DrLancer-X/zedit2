package zedit2;

public interface ConverterCallback {
    void converted(int checkVal, int x, int y, int id, int col, int chr, int vcol);
    void finished(int checkVal);
}
