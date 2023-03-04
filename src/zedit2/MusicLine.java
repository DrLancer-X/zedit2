package zedit2;

public class MusicLine {
    String seq;
    int start;
    int end;
    int linestart;

    public MusicLine(String seq, int start, int end, int linestart)
    {
        this.seq = seq;
        this.start = start;
        this.end = end;
        this.linestart = linestart;
    }
}
