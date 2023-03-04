package zedit2;

public class AudioTiming {
    private int seq, pos, len, bytes;
    public AudioTiming(int seq, int pos, int len, int bytes) {
        this.seq = seq;
        this.pos = pos;
        this.len = len;
        this.bytes = bytes;
    }
    public int getSeq() {
        return seq;
    }
    public int getPos() {
        return pos;
    }
    public int getLen() {
        return len;
    }
    public int getBytes() {
        return bytes;
    }
}
