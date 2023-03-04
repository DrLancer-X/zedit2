package zedit2;

import java.util.Arrays;
import java.util.List;

public class Stat {
    private int statId;
    private int x = -1, y = -1;
    private int stepX, stepY;
    private int cycle;
    private int p1, p2, p3;
    private int follower, leader;
    private int uid, uco;
    private int pointer;
    private int ip;
    private int codeLength;
    private int internalTag;

    private byte[] padding;
    private byte[] code;

    public Stat(boolean szzt)
    {
        stepX = 0;
        stepY = 0;
        cycle = 1;
        p1 = 0;
        p2 = 0;
        p3 = 0;
        statId = -1;
        follower = -1;
        leader = -1;
        uid = 0;
        uco = 0;
        pointer = 0;
        ip = 0;
        codeLength = 0;
        padding = new byte[szzt ? 0 : 8];
        code = new byte[0];
    }

    public Stat(byte[] worldData, int offset, int paddingSize, int statId) {
        this.statId = statId;
        x = Util.getInt8(worldData, offset + 0);
        y = Util.getInt8(worldData, offset + 1);
        stepX = Util.getInt16(worldData, offset + 2);
        stepY = Util.getInt16(worldData, offset + 4);
        cycle = Util.getInt16(worldData, offset + 6);
        p1 = Util.getInt8(worldData, offset + 8);
        p2 = Util.getInt8(worldData, offset + 9);
        p3 = Util.getInt8(worldData, offset + 10);
        follower = Util.getInt16(worldData, offset + 11);
        leader = Util.getInt16(worldData, offset + 13);
        uid = Util.getInt8(worldData, offset + 15);
        uco = Util.getInt8(worldData, offset + 16);
        pointer = Util.getInt32(worldData, offset + 17);
        ip = Util.getInt16(worldData, offset + 21);
        codeLength = Util.getInt16(worldData, offset + 23);
        internalTag = 0;

        padding = Arrays.copyOfRange(worldData, offset + 25, offset + 25 + paddingSize);
        if (codeLength > 0) {
            int codeOffset = offset + 25 + paddingSize;
            code = Arrays.copyOfRange(worldData, codeOffset, codeOffset + codeLength);
        } else {
            code = new byte[0];
        }
        migrateOldZeditInfo();
    }

    public Stat clone() {
        var newStat = new Stat(true); // Passing szzt=true to avoid padding
        copyTo(newStat);
        return newStat;
    }
    public void copyTo(Stat other) {
        other.statId = statId;
        other.x = x;
        other.y = y;
        other.stepX = stepX;
        other.stepY = stepY;
        other.cycle = cycle;
        other.p1 = p1;
        other.p2 = p2;
        other.p3 = p3;
        other.follower = follower;
        other.leader = leader;
        other.uid = uid;
        other.uco = uco;
        other.pointer = pointer;
        other.ip = ip;
        other.codeLength = codeLength;
        other.padding = padding.clone();
        other.code = code.clone();
        other.internalTag = internalTag;
    }
    public int write(byte[] worldData, int offset) {
            Util.setInt8(worldData, offset + 0, x);
            Util.setInt8(worldData, offset + 1, y);
            Util.setInt16(worldData, offset + 2, stepX);
            Util.setInt16(worldData, offset + 4, stepY);
            Util.setInt16(worldData, offset + 6, cycle);
            Util.setInt8(worldData, offset + 8, p1);
            Util.setInt8(worldData, offset + 9, p2);
            Util.setInt8(worldData, offset + 10, p3);
            Util.setInt16(worldData, offset + 11, follower);
            Util.setInt16(worldData, offset + 13, leader);
            Util.setInt8(worldData, offset + 15, uid);
            Util.setInt8(worldData, offset + 16, uco);
            Util.setInt32(worldData, offset + 17, pointer);
            Util.setInt16(worldData, offset + 21, ip);
            Util.setInt16(worldData, offset + 23, codeLength);
            System.arraycopy(padding, 0, worldData, offset + 25, padding.length);
            offset = offset + 25 + padding.length;
            if (codeLength > 0) {
                System.arraycopy(code, 0, worldData, offset, codeLength);
                offset += codeLength;
            }

        return offset;
    }

    public int getStatSize() {
        return 25 + padding.length + code.length;
    }

    public int getStatId() { return statId; }

    public void setStatId(int statId) {
        this.statId = statId;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getStepX() {
        return stepX;
    }

    public void setStepX(int stepX) {
        this.stepX = stepX;
    }

    public int getStepY() {
        return stepY;
    }

    public void setStepY(int stepY) {
        this.stepY = stepY;
    }

    public int getCycle() {
        return cycle;
    }

    public void setCycle(int cycle) {
        this.cycle = cycle;
    }

    public int getP1() {
        return p1;
    }

    public void setP1(int p1) {
        this.p1 = p1;
    }

    public int getP2() {
        return p2;
    }

    public void setP2(int p2) {
        this.p2 = p2;
    }

    public int getP3() {
        return p3;
    }

    public void setP3(int p3) {
        this.p3 = p3;
    }

    public int getFollower() {
        return follower;
    }

    public void setFollower(int follower) {
        this.follower = follower;
    }

    public int getLeader() {
        return leader;
    }

    public void setLeader(int leader) {
        this.leader = leader;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getUco() {
        return uco;
    }

    public void setUco(int uco) {
        this.uco = uco;
    }

    public int getPointer() {
        return pointer;
    }

    public void setPointer(int value) {
        this.pointer = value;
    }

    public int getIp() {
        return ip;
    }

    public void setIp(int ip) {
        this.ip = ip;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        if (codeLength > 0) {
            throw new RuntimeException("Can't set codeLength to >0. Use setCode() instead.");
        }
        this.codeLength = codeLength;
        this.code = new byte[0];
    }

    public byte[] getPadding() {
        return padding;
    }

    public void setPadding(byte[] padding) {
        this.padding = padding.clone();
    }

    public byte[] getCode() {
        return code;
    }

    public void setCode(byte[] code) {
        this.code = code.clone();
        codeLength = code.length;
    }

    /*
        original zedit padding bytes:
        0#1: zspecial (0=normal, 1=clone, 2=bindable)
        4#2: priority (now order)
        6#2: zedit tag (28159)

        zedit2 special pointer data:
        & 0xFFF00000 = (0xA6500000)
        & 0x000F0000 = flags
        & 0x0000FFFF = order

        Flags:
        1000 0x00080000 (mask=0111  0xFFF7FFFF 7) - autobind
        0100 0x00040000 (mask=1011  0xFFFBFFFF B) - specifyId
        0010 0x00020000 (mask=1101  0xFFFDFFFF D) -
        0001 0x00010000 (mask=1110  0xFFFEFFFF E) -
    */

    private void migrateOldZeditInfo() {
        // The MZX zedit used special codes in the padding data to store priority (and other things)
        // Migrate priority and use it as 'order' here.
        if (padding.length < 8) return;
        if (Util.getInt16(padding, 6) == 28159) { // We have zedit padding bytes
            int priority = Util.getInt16(padding, 4);
            int zspecial = Util.getInt8(padding, 0);
            setOrder(priority);
            setAutobind(zspecial == 2);

            // Now that we have copied this over, zero the padding
            Arrays.fill(padding, (byte) 0);
        }
    }

    public boolean isZedit2Special() {
        return ((getPointer() & 0xFFF00000) == 0xA6500000);
    }
    public void setZedit2Special() {
        if (!isZedit2Special()) {
            setPointer(0xA6500000);
        }
    }
    public int getOrder() {
        if (isZedit2Special()) {
            return (short) (getPointer() & 0xFFFF);
        }
        return 0;
    }
    public void setOrder(int order) {
        setZedit2Special();
        setPointer((getPointer() & 0xFFFF0000) | ((short) order & 0xFFFF));
    }
    public boolean isAutobind() {
        if (isZedit2Special()) return (getPointer() & 0x00080000) != 0;
        return false;
    }
    public void setAutobind(boolean flag) {
        setZedit2Special();
        setPointer((getPointer() & 0xFFF7FFFF) | (flag ? 0x00080000 : 0));
    }
    public boolean isSpecifyId() {
        if (isZedit2Special()) return (getPointer() & 0x00040000) != 0;
        return false;
    }
    public void setSpecifyId(boolean flag) {
        setZedit2Special();
        setPointer((getPointer() & 0xFFFBFFFF) | (flag ? 0x00040000 : 0));
    }
    public boolean isPlayer() {
        if (isZedit2Special()) return (getPointer() & 0x00020000) != 0;
        return false;
    }
    public void setIsPlayer(boolean flag) {
        setZedit2Special();
        setPointer((getPointer() & 0xFFFDFFFF) | (flag ? 0x00020000 : 0));
    }
    public boolean isFlag4() {
        if (isZedit2Special()) return (getPointer() & 0x00010000) != 0;
        return false;
    }
    public void setFlag4(boolean flag) {
        setZedit2Special();
        setPointer((getPointer() & 0xFFFEFFFF) | (flag ? 0x00010000 : 0));
    }

    public int getInternalTag() {
        return internalTag;
    }

    public void setInternalTag(int internalTag) {
        this.internalTag = internalTag;
    }

    public String getName() {
        if (code.length == 0) return "";
        if (code[0] != (byte)('@')) return "";
        int nameLen = 0;
        for (int i = 1; i < code.length; i++) {
            if (code[i] == '\r') {
                nameLen = i - 1;
                break;
            }
        }
        return CP437.toUnicode(Arrays.copyOfRange(code, 1, 1 + nameLen));
    }

    public boolean isIdenticalTo(Stat replacementStat) {
        return (x == replacementStat.x) &&
                (y == replacementStat.y) &&
                equals(replacementStat);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Stat)) return false;
        Stat replacementStat = (Stat)obj;
        return (p1 == replacementStat.p1) &&
               (p2 == replacementStat.p2) &&
               (p3 == replacementStat.p3) &&
               (stepX == replacementStat.stepX) &&
               (stepY == replacementStat.stepY) &&
               (cycle == replacementStat.cycle) &&
               (follower == replacementStat.follower) &&
               (leader == replacementStat.leader) &&
               (uid == replacementStat.uid) &&
               (uco == replacementStat.uco) &&
               (ip == replacementStat.ip) &&
               (pointer == replacementStat.pointer) &&
               (codeLength == replacementStat.codeLength) &&
               (Arrays.equals(padding, replacementStat.padding)) &&
               (Arrays.equals(code, replacementStat.code));
    }
}
