package zedit2;

public class Undo {
    public boolean insert;
    public String text;
    public int pos;
    public int caret;

    public Undo(boolean insert, int pos, String text, int caret) {
        this.insert = insert;
        this.pos = pos;
        this.text = text;
        this.caret = caret;
    }
}
